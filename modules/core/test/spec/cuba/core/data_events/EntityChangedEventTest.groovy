package spec.cuba.core.data_events

import com.haulmont.cuba.core.app.events.EntityChangedEvent
import com.haulmont.cuba.core.global.*
import com.haulmont.cuba.security.app.EntityLog
import com.haulmont.cuba.testmodel.sales_1.Order
import com.haulmont.cuba.testmodel.sales_1.TestEntityChangedEventListener
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
        listener.entityChangedEvents.clear()

        metadata = cont.metadata()
        events = AppBeans.get(Events)
        dataManager = AppBeans.get(DataManager)
        entityStates = AppBeans.get(EntityStates)

        AppBeans.get(EntityLog)

        listener.clear()
    }

    void cleanup() {
        listener.clear()
    }

    def "create/update/delete entity"() {

        Order order = metadata.create(Order)
        order.setNumber('111')
        order.setAmount(10)

        when:

        Order order1 = dataManager.commit(order)

        then:

        listener.entityChangedEvents.size() == 2

        !listener.entityChangedEvents[0].committedToDb
        listener.entityChangedEvents[1].committedToDb

        listener.entityChangedEvents[0].event.getEntityId() == order.id
        listener.entityChangedEvents[1].event.getEntityId() == order.id

        listener.entityChangedEvents[0].event.getType() == EntityChangedEvent.Type.CREATED
        listener.entityChangedEvents[1].event.getType() == EntityChangedEvent.Type.CREATED

        when:

        listener.clear()

        order1.setAmount(20)
        Order order2 = dataManager.commit(order1)

        then:

        listener.entityChangedEvents.size() == 2

        listener.entityChangedEvents[0].event.getEntityId() == order.id
        listener.entityChangedEvents[1].event.getEntityId() == order.id

        listener.entityChangedEvents[0].event.getType() == EntityChangedEvent.Type.UPDATED
        listener.entityChangedEvents[1].event.getType() == EntityChangedEvent.Type.UPDATED

        listener.entityChangedEvents[0].event.getChanges().attributes.contains('amount')
        listener.entityChangedEvents[0].event.getChanges().getOldValue('amount') == 10
        listener.entityChangedEvents[1].event.getChanges().attributes.contains('amount')
        listener.entityChangedEvents[1].event.getChanges().getOldValue('amount') == 10

        when:

        listener.clear()

        Order order3 = dataManager.remove(order2)

        then:

        listener.entityChangedEvents.size() == 2

        listener.entityChangedEvents[0].event.getEntityId() == order.id
        listener.entityChangedEvents[1].event.getEntityId() == order.id

        listener.entityChangedEvents[0].event.getType() == EntityChangedEvent.Type.DELETED
        listener.entityChangedEvents[1].event.getType() == EntityChangedEvent.Type.DELETED

        cleanup:

        cont.deleteRecord(order)
    }
}
