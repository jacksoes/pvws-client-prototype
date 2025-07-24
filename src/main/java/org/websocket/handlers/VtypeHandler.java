package org.websocket.handlers;

import org.epics.vtype.*;
//import org.epics.util.time.Timestamp;
//import org.epics.util.time.TimestampFormat;


        import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.Display;
import org.epics.util.stats.Range;

import java.text.NumberFormat;

import org.epics.util.text.NumberFormats;
import org.websocket.models.PV;

import java.time.Instant;

public class VtypeHandler {


    public VtypeHandler() {

    }

    public VType processUpdate(PV pvObj) {
        try {
            Object value = pvObj.getValue();
            if (value == null) {
                System.out.println("PV has null value: " + pvObj.getPv());
                return null;
            }

            //Extract fields
            String pvName = pvObj.getPv();
            String severityStr = pvObj.getSeverity();
            String description = pvObj.getDescription();
            String units = pvObj.getUnits();
            int precision = pvObj.getPrecision();
            int seconds = pvObj.getSeconds();
            int nanos = pvObj.getNanos();
            double min = pvObj.getMin();
            double max = pvObj.getMax();
            double warnLow = pvObj.getWarn_low();
            double warnHigh = pvObj.getWarn_high();
            double alarmLow = pvObj.getAlarm_low();
            double alarmHigh = pvObj.getAlarm_high();

            // Alarm
            Alarm alarm;
            try {
                AlarmSeverity severity = (severityStr != null) ? AlarmSeverity.valueOf(severityStr) : AlarmSeverity.NONE;
                alarm = Alarm.of(severity, AlarmStatus.NONE, description != null ? description : "");
            } catch (Exception e) {
                System.err.println("Alarm.none()");
                alarm = Alarm.none();
            }

            // Time
            Time time;
            try {
                Instant instant = Instant.ofEpochSecond(seconds, nanos);
                time = Time.of(instant);
            } catch (Exception e) {
                System.err.println("Time.now()");
                time = Time.now();
            }

            // Display
            Display display;
            try {
                Range displayRange = Range.of(min, max);
                Range warningRange = Range.of(warnLow, warnHigh);
                Range alarmRange = Range.of(alarmLow, alarmHigh);
                Range controlRange = displayRange;

                NumberFormat numberFormat = NumberFormats.precisionFormat(precision != 0 ? precision : 2);
                String unitStr = (units != null) ? units : "";

                display = Display.of(displayRange, alarmRange, warningRange, controlRange, unitStr, numberFormat, description);
            } catch (Exception e) {
                System.err.println("Display.none()");
                display = Display.none();
            }

            // VType Conversion
            VType vValue = VType.toVType(value, alarm, time, display);
            if (vValue == null) {
                System.out.println("Could not convert PV to VType: " + pvName);
            } else {
                System.out.println("Converted to VType: " + vValue);
            }

            return vValue;

        } catch (Exception e) {
            System.err.println("Failed to process 'update' message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

