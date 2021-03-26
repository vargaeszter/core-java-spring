package eu.arrowhead.core.plantdescriptionengine.alarms;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlarmTest {

    @Test
    public void shouldHaveProperDescriptions() {
        final String systemName = "A";
        final String systemId = "123";
        final Alarm alarmFromNamedSystem = new Alarm(null, systemName, null, AlarmCause.SYSTEM_NOT_REGISTERED);
        final Alarm alarmFromUnnamedSystem = new Alarm(systemId, null, null, AlarmCause.SYSTEM_NOT_IN_DESCRIPTION);

        assertEquals("System named '" + systemName + "' cannot be found in the Service Registry.",
            alarmFromNamedSystem.getDescription());
        assertEquals("System with ID '" + systemId + "' is not present in the active Plant Description.",
            alarmFromUnnamedSystem.getDescription());
    }

    @Test
    public void shouldRejectNullAlarmCause() {
        final Exception exception = assertThrows(RuntimeException.class, () -> new Alarm("XYZ", null, null, null));
        assertEquals("Expected an alarm cause.", exception.getMessage());
    }

    @Test
    public void shouldMatch() {
        final String systemName = "ABC";
        final String systemId = "123";

        final Alarm alarmA = new Alarm(null, systemName, null, AlarmCause.SYSTEM_NOT_REGISTERED);
        final Alarm alarmB = new Alarm(systemId, null, null, AlarmCause.SYSTEM_NOT_REGISTERED);
        final Alarm alarmC = new Alarm(systemId, systemName, null, AlarmCause.SYSTEM_NOT_REGISTERED);
        final Alarm alarmD = new Alarm(systemId, systemName, Map.of("a", "1"), AlarmCause.SYSTEM_NOT_REGISTERED);

        assertTrue(alarmA.matches(null, systemName, null, AlarmCause.SYSTEM_NOT_REGISTERED));
        assertTrue(alarmB.matches(systemId, null, null, AlarmCause.SYSTEM_NOT_REGISTERED));
        assertTrue(alarmC.matches(systemId, systemName, null, AlarmCause.SYSTEM_NOT_REGISTERED));
        assertTrue(alarmD.matches(systemId, systemName, Map.of("a", "1"), AlarmCause.SYSTEM_NOT_REGISTERED));
    }

    @Test
    public void shouldNotMatch() {
        final String systemName = "ABC";
        final String systemId = "123";

        final Alarm alarmA = new Alarm(null, systemName, null, AlarmCause.SYSTEM_NOT_REGISTERED);
        final Alarm alarmB = new Alarm(systemId, null, null, AlarmCause.SYSTEM_NOT_REGISTERED);
        final Alarm alarmC = new Alarm(systemId, systemName, null, AlarmCause.SYSTEM_NOT_REGISTERED);
        final Alarm alarmD = new Alarm(systemId, systemName, Map.of("x", "y"), AlarmCause.SYSTEM_NOT_REGISTERED);

        assertFalse(alarmA.matches("Incorrect name", systemName, null, AlarmCause.SYSTEM_NOT_REGISTERED));
        assertFalse(alarmB.matches(systemId, "Incorrect ID", null, AlarmCause.SYSTEM_NOT_REGISTERED));
        assertFalse(alarmC.matches(systemId, systemName, null, AlarmCause.SYSTEM_NOT_IN_DESCRIPTION));
        assertFalse(alarmC.matches(null, null, null, AlarmCause.SYSTEM_NOT_REGISTERED));
        assertFalse(alarmD.matches(null, null, null, AlarmCause.SYSTEM_NOT_REGISTERED));
        assertFalse(alarmD.matches(null, null, Map.of("x", "z"), AlarmCause.SYSTEM_NOT_REGISTERED));
    }

}