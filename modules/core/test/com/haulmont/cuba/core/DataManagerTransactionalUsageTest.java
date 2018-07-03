package com.haulmont.cuba.core;

import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.sys.persistence.EntityAttributeChanges;
import com.haulmont.cuba.testmodel.sales_1.OrderLine;
import com.haulmont.cuba.testmodel.sales_1.Product;
import com.haulmont.cuba.testsupport.TestContainer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.inject.Inject;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class DataManagerTransactionalUsageTest {

    @Component("test_SaleProcessor")
    public static class SaleProcessor {

        @Inject
        private TransactionalDataManager dataManager;

        @Inject
        private Metadata metadata;

        @Transactional
        public Id<OrderLine, UUID> sell(String productName, Integer quantity) {
            Product product = dataManager.load(Product.class)
                    .query("select p from sales1$Product p where p.name = :name")
                    .parameter("name", productName)
                    .optional()
                    .orElseGet(() -> {
                        Product p = metadata.create(Product.class);
                        p.setName(productName);
                        p.setQuantity(100); // initial quantity of a new product
                        return dataManager.save(p);
                    });

            OrderLine orderLine = metadata.create(OrderLine.class);
            orderLine.setProduct(product);
            orderLine.setQuantity(quantity);
            dataManager.save(orderLine);

            return Id.of(orderLine);
        }

    }

    @Component("test_OrderLineChangedListener")
    public static class OrderLineChangedListener {

        @Inject
        private TransactionalDataManager dataManager;

        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
//        @EventListener
        public void onChanged(EntityChangedEvent<OrderLine> event) {
            OrderLine orderLine = dataManager.load(OrderLine.class).id(event.getEntity().getId())
                    .view("with-product").one();

            Product product = orderLine.getProduct();

            if (event.getType() == EntityChangedEvent.Type.UPDATED) {
                EntityAttributeChanges changes = event.getChanges();

                if (changes.isChanged("product")) {
                    Product oldProduct = changes.getOldValue("product");
                    oldProduct.setQuantity(oldProduct.getQuantity() + orderLine.getQuantity());

                    product.setQuantity(product.getQuantity() - orderLine.getQuantity());
                    dataManager.save(oldProduct, product);

                } else if (changes.isChanged("quantity")) {
                    product.setQuantity(product.getQuantity() - orderLine.getQuantity());
                    dataManager.save(product);
                }

            } else if (event.getType() == EntityChangedEvent.Type.CREATED) {
                product.setQuantity(product.getQuantity() - orderLine.getQuantity());
                dataManager.save(product);

            } else if (event.getType() == EntityChangedEvent.Type.DELETED) {
                product.setQuantity(product.getQuantity() + orderLine.getQuantity());
                dataManager.save(product);
            }
        }
    }

    @ClassRule
    public static TestContainer cont = TestContainer.Common.INSTANCE;

    protected DataManager dataManager;
    private Metadata metadata;
    private Persistence persistence;

    @Before
    public void setUp() throws Exception {
        dataManager = AppBeans.get(DataManager.class);
        metadata = cont.metadata();
        persistence = cont.persistence();

        QueryRunner runner = new QueryRunner(persistence.getDataSource());
        runner.update("delete from SALES1_ORDER_LINE");
        runner.update("delete from SALES1_PRODUCT");
    }

    @Test
    public void test() {
        SaleProcessor processor = AppBeans.get("test_SaleProcessor");
        Id<OrderLine, UUID> orderLineId = processor.sell("abc", 10);

        Product product = dataManager.load(Product.class)
                .query("select p from sales1$Product p where p.name = :name")
                .parameter("name", "abc")
                .one();
        assertEquals(90, (int) product.getQuantity());
    }
}
