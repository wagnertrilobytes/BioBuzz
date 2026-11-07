package org.firstinspires.ftc.teamcode.version1;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Looks up the eight Version 1 motors, stops them, and records what it found.
 *
 * A missing name, a duplicate name, or a failed lookup leaves that motor null.
 * Null motors are never commanded. Directions are applied only when the matching
 * confirmation flag is true and the direction field is not null.
 */
public class Version1Hardware {

    public DcMotor frontLeft;
    public DcMotor backLeft;
    public DcMotor frontRight;
    public DcMotor backRight;
    public DcMotor intake;
    public DcMotor feed;
    public DcMotor launcher1;
    public DcMotor launcher2;

    public boolean namesAreUnique = true;

    private final ArrayList<String> mapReport = new ArrayList<String>();

    public void init(HardwareMap hardwareMap) {
        mapReport.clear();
        namesAreUnique = true;

        String frontLeftName = clean(Version1Config.FRONT_LEFT_NAME);
        String backLeftName = clean(Version1Config.BACK_LEFT_NAME);
        String frontRightName = clean(Version1Config.FRONT_RIGHT_NAME);
        String backRightName = clean(Version1Config.BACK_RIGHT_NAME);
        String intakeName = clean(Version1Config.INTAKE_NAME);
        String feedName = clean(Version1Config.FEED_NAME);
        String launcher1Name = clean(Version1Config.LAUNCHER_1_NAME);
        String launcher2Name = clean(Version1Config.LAUNCHER_2_NAME);

        Map<String, String> used = new LinkedHashMap<String, String>();
        boolean unique = true;
        unique = noteName(used, "FL", frontLeftName) && unique;
        unique = noteName(used, "BL", backLeftName) && unique;
        unique = noteName(used, "FR", frontRightName) && unique;
        unique = noteName(used, "BR", backRightName) && unique;
        unique = noteName(used, "intake", intakeName) && unique;
        unique = noteName(used, "feed", feedName) && unique;
        unique = noteName(used, "launcher1", launcher1Name) && unique;
        unique = noteName(used, "launcher2", launcher2Name) && unique;
        namesAreUnique = unique;
        if (!namesAreUnique) {
            mapReport.add("DUPLICATE names: all motion stays blocked");
        }

        frontLeft = lookup(hardwareMap, "FL", frontLeftName);
        backLeft = lookup(hardwareMap, "BL", backLeftName);
        frontRight = lookup(hardwareMap, "FR", frontRightName);
        backRight = lookup(hardwareMap, "BR", backRightName);
        intake = lookup(hardwareMap, "intake", intakeName);
        feed = lookup(hardwareMap, "feed", feedName);
        launcher1 = lookup(hardwareMap, "launcher1", launcher1Name);
        launcher2 = lookup(hardwareMap, "launcher2", launcher2Name);

        applyDirection(frontLeft, Version1Config.FRONT_LEFT_DIRECTION, "FL", Version1Config.DRIVE_DIRECTIONS_CONFIRMED);
        applyDirection(backLeft, Version1Config.BACK_LEFT_DIRECTION, "BL", Version1Config.DRIVE_DIRECTIONS_CONFIRMED);
        applyDirection(frontRight, Version1Config.FRONT_RIGHT_DIRECTION, "FR", Version1Config.DRIVE_DIRECTIONS_CONFIRMED);
        applyDirection(backRight, Version1Config.BACK_RIGHT_DIRECTION, "BR", Version1Config.DRIVE_DIRECTIONS_CONFIRMED);
        applyDirection(intake, Version1Config.INTAKE_DIRECTION, "intake", Version1Config.MECHANISM_DIRECTIONS_CONFIRMED);
        applyDirection(feed, Version1Config.FEED_DIRECTION, "feed", Version1Config.MECHANISM_DIRECTIONS_CONFIRMED);
        applyDirection(launcher1, Version1Config.LAUNCHER_1_DIRECTION, "launcher1", Version1Config.MECHANISM_DIRECTIONS_CONFIRMED);
        applyDirection(launcher2, Version1Config.LAUNCHER_2_DIRECTION, "launcher2", Version1Config.MECHANISM_DIRECTIONS_CONFIRMED);

        zeroAll();
    }

    public void zeroAll() {
        command(frontLeft, 0.0);
        command(backLeft, 0.0);
        command(frontRight, 0.0);
        command(backRight, 0.0);
        command(intake, 0.0);
        command(feed, 0.0);
        command(launcher1, 0.0);
        command(launcher2, 0.0);
    }

    public void command(DcMotor motor, double power) {
        if (motor == null) {
            return;
        }
        if (Double.isNaN(power) || Double.isInfinite(power)) {
            power = 0.0;
        }
        if (power > 1.0) {
            power = 1.0;
        }
        if (power < -1.0) {
            power = -1.0;
        }
        motor.setPower(power);
    }

    public void addMapReport(Telemetry telemetry) {
        for (String line : mapReport) {
            telemetry.addLine(line);
        }
    }

    public List<String> mapReportLines() {
        return mapReport;
    }

    private boolean noteName(Map<String, String> used, String role, String name) {
        if (name.isEmpty()) {
            return true;
        }
        String previous = used.put(name, role);
        if (previous != null) {
            mapReport.add("DUPLICATE \"" + name + "\" on " + previous + " and " + role);
            return false;
        }
        return true;
    }

    private DcMotor lookup(HardwareMap hardwareMap, String role, String name) {
        if (name.isEmpty()) {
            mapReport.add(role + ": NAME UNSET");
            return null;
        }
        try {
            DcMotor motor = hardwareMap.get(DcMotor.class, name);
            motor.setPower(0.0);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            if (Version1Config.ZERO_POWER_BEHAVIOR != null) {
                motor.setZeroPowerBehavior(Version1Config.ZERO_POWER_BEHAVIOR);
            } else {
                mapReport.add(role + ": zero-power behavior left at hub default");
            }
            motor.setPower(0.0);
            mapReport.add(role + ": FOUND \"" + name + "\"");
            return motor;
        } catch (RuntimeException ex) {
            mapReport.add(role + ": MISSING \"" + name + "\" " + ex.getClass().getSimpleName());
            return null;
        }
    }

    private void applyDirection(DcMotor motor, DcMotor.Direction direction, String role, boolean confirmed) {
        if (motor == null) {
            return;
        }
        if (!confirmed) {
            mapReport.add(role + ": direction not applied (confirmation false)");
            return;
        }
        if (direction == null) {
            mapReport.add(role + ": direction UNSET");
            return;
        }
        motor.setDirection(direction);
        mapReport.add(role + ": direction " + direction.name());
    }

    private String clean(String name) {
        if (name == null) {
            return "";
        }
        return name.trim();
    }
}
