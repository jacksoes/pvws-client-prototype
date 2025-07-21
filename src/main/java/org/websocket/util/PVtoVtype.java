package org.websocket.util;

import org.websocket.models.PV;

import org.epics.vtype.*;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.Display;
import org.epics.util.stats.Range;
import java.text.NumberFormat;
import org.epics.util.text.NumberFormats;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;


public class PVtoVtype {


    //USE JACKSON TO CHECK IF FIELDS ARE NULL




        public static VType getVtype(PV pvObj) {

            //DONT WORRY ABOUT MISSING PARAMETERS
            // WILL ALWAYS BE THIS CASE: toVType(Object javaObject, Alarm alarm, Time time, Display display)

            // CAN MAKE UNIT TESTS FOR CERTAIN INPUTS INTO toVType

            Object value = pvObj.getValue(); //extract all field into local variable

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


            Alarm alarm;
            try {
                AlarmSeverity severity = (severityStr != null) ? AlarmSeverity.valueOf(severityStr) : AlarmSeverity.NONE;
                alarm = Alarm.of(severity, AlarmStatus.NONE, description != null ? description : "");
            } catch (Exception e) {
                System.err.println("Alarm.none()");
                alarm = Alarm.none();
            }

            if(severityStr == null)
            {
                return VType.toVType(value);
            }
            else if(alarmLow == 0.0)
            {
                return VType.toVType(value, alarm);
            }

            return VType.toVType(value);


    /*
            Alarm alarm;
            try {
                AlarmSeverity severity = (severityStr != null) ? AlarmSeverity.valueOf(severityStr) : AlarmSeverity.NONE;
                alarm = Alarm.of(severity, AlarmStatus.NONE, description != null ? description : "");
            } catch (Exception e) {
                System.err.println("Alarm.none()");
                alarm = Alarm.none();
            }

            Time time;
            try {
                Instant instant = Instant.ofEpochSecond(seconds, nanos);
                time = Time.of(instant);
            } catch (Exception e) {
                System.err.println("Time.now()");
                time = Time.now();
            }


            Display display;
            try {
                Range displayRange = Range.of(min, max);
                Range warningRange = Range.of(warnLow, warnHigh);
                Range alarmRange = Range.of(alarmLow, alarmHigh);
                Range controlRange = displayRange;

                String unitStr = units != null ? units : "";
                NumberFormat numberFormat = NumberFormats.precisionFormat(precision != 0 ? precision : 2);

                display = Display.of(displayRange, alarmRange, warningRange, controlRange, units != null ? units : "", unitStr, description, numberFormat);
            } catch (Exception e) {
                System.err.println("Display.none()");
                display = Display.none();
            }

            //Object Vvalue = VType.toVType(pvObj.getValue(), alarm, time, display);
            //pvObj.setValue(value);
            VType vValue;
            try {
                vValue = VType.toVType(value, alarm, time, display);
                if (vValue == null) {
                    System.out.println("Could not convert PV to VType: " + pvName);
                } else {
                    System.out.println("Converted to VType: " + vValue);
                    // TODO: handle/store/display vValue
                } catch(Exception e){
                    System.err.println("Error during VType conversion: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Failed to process 'update' message: " + e.getMessage());
                e.printStackTrace();
            }

     */


        }


}
