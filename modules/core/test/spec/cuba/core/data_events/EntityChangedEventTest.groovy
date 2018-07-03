package spec.cuba.core.data_events

import com.haulmont.cuba.core.EntityChangedEvent
import com.haulmont.cuba.core.global.*
import com.haulmont.cuba.testmodel.sales_1.TestEntityChangedEventListener
import com.haulmont.cuba.testmodel.sales_1.TestEntityChangedEventListener.Info
import com.haulmont.cuba.testmodel.sales_1.Customer
import com.haulmont.cuba.testmodel.sales_1.Order
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

        Order order = metadata.create(Order)
        order.setNumber('111')
        order.setAmount(10)

        when:

        Order order1 = dataManager.commit(order)

        then:

        listener.received.size() == 2

        checkEventInfo(listener.received[0])
        checkEventInfo(listener.received[1])

        !listener.received[0].committedToDb
        listener.received[1].committedToDb

        listener.received[0].event.getEntity() == order
        listener.received[1].event.getEntity() == order

        listener.received[0].event.getType() == EntityChangedEvent.Type.CREATED
        listener.received[1].event.getType() == EntityChangedEvent.Type.CREATED

        when:

        listener.received.clear()

        order1.setAmount(20)
        Order order2 = dataManager.commit(order1)

        then:

        listener.received.size() == 2

        checkEventInfo(listener.received[0])
        checkEventInfo(listener.received[1])

        listener.received[0].event.getEntity() == order
        listener.received[1].event.getEntity() == order

        listener.received[0].event.getType() == EntityChangedEvent.Type.UPDATED
        listener.received[1].event.getType() == EntityChangedEvent.Type.UPDATED

        listener.received[0].event.getChanges().attributes.contains('amount')
        listener.received[0].event.getChanges().getOldValue('amount') == 10
        listener.received[1].event.getChanges().attributes.contains('amount')
        listener.received[1].event.getChanges().getOldValue('amount') == 10

        when:

        listener.received.clear()

        Order order3 = dataManager.remove(order2)

        then:

        listener.received.size() == 2

        checkEventInfo(listener.received[0])
        checkEventInfo(listener.received[1])

        listener.received[0].event.getEntity() == order
        listener.received[1].event.getEntity() == order

        listener.received[0].event.getType() == EntityChangedEvent.Type.DELETED
        listener.received[1].event.getType() == EntityChangedEvent.Type.DELETED

        cleanup:

        cont.deleteRecord(order)
    }

    private void checkEventInfo(Info info) {
        assert info.detached && !info.inPersistenceContext
    }

    def "entity in event has correct view"() {

        Customer customer = metadata.create(Customer)
        customer.name = 'foo'

        Order order = metadata.create(Order)
        order.setNumber('111')
        order.setAmount(10)
        order.setCustomer(customer)

        dataManager.commit(new CommitContext(customer, order))

        Order order1 = dataManager.load(Order).id(order.id).one()

        listener.received.clear()

        when:

        order1.setAmount(20)
        dataManager.commit(order1)

        then:

        listener.received.size() == 2

        entityStates.isLoadedWithView(listener.received[0].event.entity, 'with-customer')
        entityStates.isLoadedWithView(listener.received[1].event.entity, 'with-customer')
    }
}
