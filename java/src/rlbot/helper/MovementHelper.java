package rlbot.helper;

import rlbot.*;
import rlbot.Bot.Team;
import rlbot.strategies.StrategyController.StrategyOutput;
import rlbot.strategies.StrategyController.StrategyOutputRaw;

public class MovementHelper {
    public static AgentOutput getCorrectiveOutput(CarRotation current, CarRotation target) {
        //TODO: work out roll/pitch/yaw values to align current with target
        //first and foremost, correct forward vector
        //second, rotate to match roof vector
        float roll = 1, yaw = 0, pitch = 0;
        
        AgentOutput out = new AgentOutput();
        if(Math.abs(roll) > Math.abs(yaw)) {
            roll = roll * 10;
            if(roll < -1) roll = -1;
            if(roll > 1) roll = 1;
            out.withSlide(true).withSteer((float)roll);
        }
        else {
            yaw = yaw * 10;
            if(yaw < -1) yaw = -1;
            if(yaw > 1) yaw = 1;
            out.withSlide(false).withSteer((float)yaw);
        }
        pitch = pitch * 10;
        if(pitch < -1) pitch = -1;
        if(pitch > 1) pitch = 1;
        out.withPitch((float)pitch);
        
        out.withBoost(false);
        return out;
    }
    
    private ObjInfo lastTarget;
    
    private MovementAction curAction;
    
    public boolean canRelease(ObjInfo car) {
        if(curAction != null && !curAction.isDone(car)) return false;
        return true;
    }
    
    public AgentOutput getMovement(StrategyOutput info) {
        if(info.target == null) info.target = lastTarget;
        else {
            if(info.raw) return ((StrategyOutputRaw)info).out;
            lastTarget = info.target;
        }
        
        if(curAction != null) {
            if(curAction.isDone(info.car)) curAction = null;
            else return curAction.doMovement(info.car);
        }
        
        if(info.car.pos.y > 1) {
            System.out.println("Went airborne, landing...");
            curAction = new MovementActionLand(info.target == null ? info.car.pos : info.target.pos);
            return curAction.doMovement(info.car);
        }
        
        //We've completed our movement goal, and no new movement has been given.
        //This is purely a precaution, there should always be new movement.
        if(info.target == null) {
            return new AgentOutput().withAcceleration(0).withDeceleration(0).
                                     withBoost(false).withJump(false).withPitch(0).
                                     withSteer(0).withSlide(false);
        }
        
        //TODO: improve movement (account for target rotation/velocity, do flips, etc.)
        
        Vector relativePos = Vector.sub(info.target.pos, info.car.pos);
        double targetAngle = Math.atan2(relativePos.z, relativePos.x);
        double myAngle = Math.atan2(info.car.rot.forward.z, info.car.rot.forward.x);
        double diff = targetAngle - myAngle;
        
        while(diff < -Math.PI) diff += Math.PI * 2;
        while(diff >  Math.PI) diff -= Math.PI * 2;
        
        double turnAngle = 0;
        if(Math.abs(diff) < 0.05) turnAngle = diff * 2;
        else if(Math.abs(diff) < 0.2) turnAngle = diff * 5;
        else if(diff > 0) turnAngle = 1;
        else turnAngle = -1;
        
        boolean boost = false;
        System.out.println(info.car.vel.length());
        if(info.car.boost > info.target.boost && info.car.vel.length() < GameConstants.Speeds.TOP_SPEED_BOOST) boost = true;
        return new AgentOutput().withAcceleration(1).withSteer((float)turnAngle).withBoost(boost);
    }
}