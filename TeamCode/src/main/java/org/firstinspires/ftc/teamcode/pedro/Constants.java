package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.follower.Follower;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.Encoder;
//import com.pedropathing.revhub.localizers.ThreeWheelConstants;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

//import org.firstinspires.ftc.teamcode.helpers.GlobalStorage;

public class Constants {

    public static MecanumConfig drivetrainConfig = new MecanumConfig(c -> {
        c.frontLeftName.set("frontLeft");
        c.frontRightName.set("frontRight");
        c.backLeftName.set("backLeft");
        c.backRightName.set("backRight");
        c.frontLeftDirection.set(DcMotorSimple.Direction.REVERSE);
        c.frontRightDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backLeftDirection.set(DcMotorSimple.Direction.REVERSE);
        c.backRightDirection.set(DcMotorSimple.Direction.FORWARD);
    });

   // public static ThreeWheelConstants localizerConstants = new ThreeWheelConstants()
            // https://pedropathing.com/_next/static/media/odometry.528df183.png
       //     .leftPodY(-4)
        //    .rightPodY(-4)
         //   .strafePodX(5)
         //   .forwardTicksToInches(0.00148165)
         //   .strafeTicksToInches(0.00143046)
         //   .turnTicksToInches(0.0028503306212668967)
   //         .leftEncoder_HardwareMapName(GlobalStorage.HardwareConstants.DRIVE_BACK_LEFT.getIdentifier())
     //       .rightEncoder_HardwareMapName(GlobalStorage.HardwareConstants.DRIVE_FRONT_RIGHT.getIdentifier())
     //       .strafeEncoder_HardwareMapName(GlobalStorage.HardwareConstants.DRIVE_FRONT_LEFT.getIdentifier())
     //       .leftEncoderDirection(Encoder.REVERSE)
     //       .rightEncoderDirection(Encoder.FORWARD)
      //      .strafeEncoderDirection(Encoder.FORWARD);

 //   public static Follower create(HardwareMap h) {
  //      return new Follower(h);
  //  }
}