package com.haulmont.cuba.testmodel.sales_1;

import com.haulmont.bali.db.ArrayHandler;
import com.haulmont.bali.db.QueryRunner;
import com.haulmont.cuba.core.EntityChangedEvent;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.global.EntityStates;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component("test_EntityChangedEventListener")
public class TestEntityChangedEventListener {

    public static class Info {
        public final EntityChangedEvent event;
        public final boolean detached;
        public final boolean committedToDb;
        public final boolean inPersistenceContext;

        public Info(EntityChangedEvent event, boolean detached, boolean committedToDb, boolean inPersistenceContext) {
            this.event = event;
            this.detached = detached;
            this.committedToDb = committedToDb;
            this.inPersistenceContext = inPersistenceContext;
        }
    }

    public List<Info> received = new ArrayList<>();

    @Inject
    private Persistence persistence;

    @Inject
    private EntityStates entityStates;

    @EventListener
    void beforeCommit(EntityChangedEvent<Order> event) {
        received.add(new Info(event,
                entityStates.isDetached(event.getEntity()),
                isCommitted(event.getEntity()),
                persistence.getEntityManager().getDelegate().contains(event.getEntity())));
    }

    @TransactionalEventListener
    void afterCommit(EntityChangedEvent<Order> event) {
        received.add(new Info(event,
                entityStates.isDetached(event.getEntity()),
                isCommitted(event.getEntity()),
                false));
    }

    private boolean isCommitted(Order entity) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            QueryRunner runner = new QueryRunner(persistence.getDataSource());
            try {
                Object[] row = runner.query("select id from SALES1_ORDER where id = ?",
                        entity.getId().toString(),
                        new ArrayHandler());
                return row != null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            return future.get(200L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            return false;
        }
    }

}
