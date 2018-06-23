package spec.cuba.core.data_events

import com.haulmont.cuba.core.EntityChangedEvent
import com.haulmont.cuba.core.global.*
import com.haulmont.cuba.data_events.TestEntityChangedEventListener
import com.haulmont.cuba.data_events.TestEntityChangedEventListener.Info
import com.haulmont.cuba.testmodel.data_events.Product
import com.haulmont.cuba.testmodel.data_events.PurchaseItem
import com.haulmont.cuba.testsupport.TestContainer
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

class EntityChangedEventTest extends Specification {

    @Shared @ClassRule
    public TestContainer cont = TestContainer.Common.INSTANCE

    private TestEntityChangedEventListener listener
    private Events events
    private DataManager dataManager
    private Metadata metadata
    private EntityStates entityStates

    void setup() {
        listener = AppBeans.get(TestEntityChangedEventListener)
        listener.received.clear()

        metadata = cont.metadata()
        events = AppBeans.get(Events)
        dataManager = AppBeans.get(DataManager)
        entityStates = AppBeans.get(EntityStates)
    }

    void cleanup() {
        listener.received.clear()
    }

    def "create/update/delete entity"() {

        PurchaseItem item = metadata.create(PurchaseItem)
        item.setNumber('111')
        item.setQuantity(10)

        when:

        PurchaseItem item1 = dataManager.commit(item)

        then:

        listener.received.size() == 2

        checkEventInfo(listener.received[0])
        checkEventInfo(listener.received[1])

        !listener.received[0].committedToDb
        listener.received[1].committedToDb

        listener.received[0].event.getEntity() == item
        listener.received[1].event.getEntity() == item

        listener.received[0].event.getType() == EntityChangedEvent.Type.CREATED
        listener.received[1].event.getType() == EntityChangedEvent.Type.CREATED

        when:

        listener.received.clear()

        item1.setQuantity(20)
        PurchaseItem item2 = dataManager.commit(item1)

        then:

        listener.received.size() == 2

        checkEventInfo(listener.received[0])
        checkEventInfo(listener.received[1])

        listener.received[0].event.getEntity() == item
        listener.received[1].event.getEntity() == item

        listener.received[0].event.getType() == EntityChangedEvent.Type.UPDATED
        listener.received[1].event.getType() == EntityChangedEvent.Type.UPDATED

        listener.received[0].event.getChanges().attributes.contains('quantity')
        listener.received[0].event.getChanges().getOldValue('quantity') == 10
        listener.received[1].event.getChanges().attributes.contains('quantity')
        listener.received[1].event.getChanges().getOldValue('quantity') == 10

        when:

        listener.received.clear()

        PurchaseItem item3 = dataManager.remove(item2)

        then:

        listener.received.size() == 2

        checkEventInfo(listener.received[0])
        checkEventInfo(listener.received[1])

        listener.received[0].event.getEntity() == item
        listener.received[1].event.getEntity() == item

        listener.received[0].event.getType() == EntityChangedEvent.Type.DELETED
        listener.received[1].event.getType() == EntityChangedEvent.Type.DELETED
    }

    private void checkEventInfo(Info info) {
        assert info.detached && !info.inPersistenceContext
    }

    def "entity in event has correct view"() {

        Product product = metadata.create(Product)
        product.name = 'foo'

        PurchaseItem item = metadata.create(PurchaseItem)
        item.setNumber('111')
        item.setQuantity(10)
        item.setProduct(product)

        dataManager.commit(new CommitContext(product, item))

        PurchaseItem item1 = dataManager.load(PurchaseItem).id(item.id).one()

        listener.received.clear()

        when:

        item1.setQuantity(20)
        dataManager.commit(item1)

        then:

        listener.received.size() == 2

        entityStates.isLoadedWithView(listener.received[0].event.entity, 'with-product')
        entityStates.isLoadedWithView(listener.received[1].event.entity, 'with-product')
    }
}
