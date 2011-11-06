///**
// * Copyright (C) 2010, 2011 by Arne Kesting, Martin Treiber,
// *                             Ralph Germ, Martin Budden
// *                             <info@movsim.org>
// * ----------------------------------------------------------------------
// * 
// *  This file is part of 
// *  
// *  MovSim - the multi-model open-source vehicular-traffic simulator 
// *
// *  MovSim is free software: you can redistribute it and/or modify
// *  it under the terms of the GNU General Public License as published by
// *  the Free Software Foundation, either version 3 of the License, or
// *  (at your option) any later version.
// *
// *  MovSim is distributed in the hope that it will be useful,
// *  but WITHOUT ANY WARRANTY; without even the implied warranty of
// *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *  GNU General Public License for more details.
// *
// *  You should have received a copy of the GNU General Public License
// *  along with MovSim.  If not, see <http://www.gnu.org/licenses/> or
// *  <http://www.movsim.org>.
// *  
// * ----------------------------------------------------------------------
// */
//package org.movsim.simulator.vehicles.impl;
//
//import java.util.List;
//
//import org.movsim.consumption.FuelConsumption;
//import org.movsim.input.model.VehicleInput;
//import org.movsim.simulator.Constants;
//import org.movsim.simulator.impl.RoadNetworkDeprecated;
//import org.movsim.simulator.roadSection.TrafficLight;
//import org.movsim.simulator.vehicles.Moveable;
//import org.movsim.simulator.vehicles.Noise;
//import org.movsim.simulator.vehicles.PhysicalQuantities;
//import org.movsim.simulator.vehicles.Vehicle;
//import org.movsim.simulator.vehicles.VehicleContainer;
//import org.movsim.simulator.vehicles.lanechanging.impl.LaneChangingModelImpl;
//import org.movsim.simulator.vehicles.longmodel.Memory;
//import org.movsim.simulator.vehicles.longmodel.TrafficLightApproaching;
//import org.movsim.simulator.vehicles.longmodel.accelerationmodels.AccelerationModel;
//import org.movsim.simulator.vehicles.longmodel.impl.MemoryImpl;
//import org.movsim.simulator.vehicles.longmodel.impl.TrafficLightApproachingImpl;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * The Class VehicleImpl.
// */
//public class VehicleImpl {
//
//    /** The Constant logger. */
//    final static Logger logger = LoggerFactory.getLogger(VehicleImpl.class);
//    
//    /** in m/s^2 */
//    private final static double THRESHOLD_BRAKELIGHT_ON = 0.2;
//    
//    /** in m/s^2 */
//    private final static double THRESHOLD_BRAKELIGHT_OFF = 0.1;
//    
//    /** needs to be > 0 */
//    private final static double FINITE_LANE_CHANGE_TIME_S = 5;
//
//    /** The label. */
//    private final String label;
//
//    /** The length. */
//    private final double length;
//
//    /** The position. */
//    private double position;
//
//    /** The old position. */
//    private double positionOld;
//
//    /** The speed. */
//    private double speed;
//
//    /** The acceleration model. */
//    private double accModel;
//
//    /** The acceleration. */
//    private double acc;
//
//    private double accOld;
//
//    /** The reaction time. */
//    private final double reactionTime;
//
//    /** The max deceleration . */
//    private final double maxDecel;
//
//    /** The id. */
//    int id;
//
//    /** The vehicle number. */
//    private int vehNumber;
//
//    /** The lane. */
//    private int lane;
//
//    private int laneOld;
//
//    /** variable for remembering new target lane when assigning to new vehContainerLane */
//    private int targetLane;
//
//    /** finite lane-changing duration */
//    private double tLaneChangingDelay;
//
//    /** The speed limit. */
//    private double speedlimit;
//
//    /** The long model. */
//    private final AccelerationModel accelerationModel;
//
//    /** The lane-changing model. */
//    private final LaneChangingModelImpl lcModel;
//
//    /** The memory. */
//    private Memory memory = null;
//
//    /** The noise. */
//    private Noise noise = null;
//
//    /** The traffic light approaching. */
//    private final TrafficLightApproaching trafficLightApproaching;
//
//    /** The cyclic buffer. */
//    private final CyclicBufferImpl cyclicBuffer; // TODO
//
//    private final FuelConsumption fuelModel; // can be null
//    
//    private boolean isBrakeLightOn;
//
//    private PhysicalQuantities physQuantities;
//    
//    
//    private long roadId;
//
//    /**
//     * Instantiates a new vehicle impl.
//     *
//     * @param label the label
//     * @param id the id
//     * @param longModel the acceleration model. longitudinal ("car-following") model.
//     * @param vehInput the veh input
//     * @param cyclicBuffer the cyclic buffer
//     * @param lcModel the lanechange model
//     */
//    public VehicleImpl(String label, int id, final AccelerationModel longModel, final VehicleInput vehInput,
//            final CyclicBufferImpl cyclicBuffer, final LaneChangingModelImpl lcModel, final FuelConsumption fuelModel) {
//        this.label = label;
//        this.id = id;
//        this.fuelModel = fuelModel;  
//
//        length = vehInput.getLength();
//        reactionTime = vehInput.getReactionTime();
//        maxDecel = vehInput.getMaxDeceleration();
//
//        initialize();
//        // longitudinal ("car-following") model
//        this.accelerationModel = longModel;
//        physQuantities = new PhysicalQuantities(this);
//
//        // lane-changing model
//        this.lcModel = lcModel;
//        lcModel.initialize(this);
//
//        this.cyclicBuffer = cyclicBuffer;
//        
//        // no effect if model is not configured with memory effect
//        if (vehInput.isWithMemory()) {
//            memory = new MemoryImpl(vehInput.getMemoryInputData());
//        }
//
//        if (vehInput.isWithNoise()) {
//            noise = new NoiseImpl(vehInput.getNoiseInputData());
//        }
//
//        trafficLightApproaching = new TrafficLightApproachingImpl();
//
//        // needs to be > 0 to avoid lane-changing over 2 lanes in one update
//        // step
//        assert FINITE_LANE_CHANGE_TIME_S > 0;
//
//    }
//    
//    private void initialize(){
//        positionOld = 0;
//        position = 0;
//        speed = 0;
//        acc = 0;
//        isBrakeLightOn = false;
//
//        speedlimit = Constants.MAX_VEHICLE_SPEED;
//    }
//    
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#init(double, double, int)
//     */
//
//    // central book-keeping of lanes (lane and laneOld)
//
//    @Override
//    public void init(double pos, double v, int lane, long roadId) {
//        this.laneOld = this.lane; // remember previous lane
//        this.roadId = roadId;
//        this.position = pos;
//        this.positionOld = pos;
//        this.speed = v;
//        // targetlane not needed anymore for book-keeping, vehicle is in new
//        // lane
//        this.targetLane = this.lane = lane;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#getLabel()
//     */
//    @Override
//    public String getLabel() {
//        return label;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#length()
//     */
//    @Override
//    public double getLength() {
//        return length;
//    }
//
//    @Override
//    public double getWidth() {
//        return Constants.VEHICLE_WIDTH;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#position()
//     */
//
//    // returns the vehicle's mid-position
//    @Override
//    public double getPosition() {
//        return position;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#posFrontBumper()
//     */
//    @Override
//    public double posFrontBumper() {
//        return position + 0.5 * length;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#posReadBumper()
//     */
//    @Override
//    public double posRearBumper() {
//        return position - 0.5 * length;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#oldPosition()
//     */
//    @Override
//    public double getPositionOld() {
//        return positionOld;
//    }
//
//    /**
//     * Sets the position.
//     * 
//     * @param position
//     *            the new position
//     */
//    @Override
//    public void setPosition(double position) {
//        this.position = position;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#speed()
//     */
//    @Override
//    public double getSpeed() {
//        return speed;
//    }
//
//    /**
//     * Sets the speed.
//     * 
//     * @param speed
//     *            the new speed
//     */
//    public void setSpeed(double speed) {
//        this.speed = speed;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#speedlimit()
//     */
//    @Override
//    public double getSpeedlimit() {
//        return speedlimit;
//    }
//
//    // externally given speedlimit
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#setSpeedlimit(double)
//     */
//    @Override
//    public void setSpeedlimit(double speedlimit) {
//        this.speedlimit = speedlimit;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#acc()
//     */
//    @Override
//    public double getAcc() {
//        return acc;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#accModel()
//     */
//    @Override
//    public double accModel() {
//        return accModel;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#distanceToTrafficlight()
//     */
//    @Override
//    public double getDistanceToTrafficlight() {
//        return trafficLightApproaching.getDistanceToTrafficlight();
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#id()
//     */
//    @Override
//    public int getId() {
//        return id;
//    }
//    
//    
//    @Override
//    public long getRoadId(){
//        return roadId;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#isFromOnramp()
//     */
//    @Override
//    public boolean isFromOnramp() {
//        // TODO not working anymore, new concept needed for determining origin
//        // of vehicle
//        return (vehNumber < 0);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#getVehNumber()
//     */
//    @Override
//    public int getVehNumber() {
//        return vehNumber;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#setVehNumber(int)
//     */
//    @Override
//    public void setVehNumber(int vehNumber) {
//        this.vehNumber = vehNumber;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.movsim.simulator.vehicles.Vehicle#netDistance(org.movsim.simulator
//     * .vehicles.Vehicle)
//     */
//    @Override
//    public double getNetDistance(final Moveable vehFront) {
//        
//        final RoadNetworkDeprecated roadNetwork = RoadNetworkDeprecated.getInstance();
//        final boolean isRingroad = roadNetwork.isPeriodBoundary(roadId);
//        final double roadLength = roadNetwork.getRoadLength(roadId); 
//        
//        if (vehFront == null) {
//            if(isRingroad){
//                return (roadLength - length); 
//            }
//            return Constants.GAP_INFINITY;
//        }
//        
//        // TODO general concept for treating offsets needed here
//        // so far use hard-coded solution
//        double netGap = vehFront.getPosition() - position - 0.5 * (getLength() + vehFront.getLength());
//        final long idOfframp=-1;
//        final long idOnramp=1;
//        if( /*roadId  != vehFront.getRoadId()*/ roadId==idOfframp && vehFront.getRoadId()==idOnramp || ( isRingroad && netGap <= -0.5 * (length + vehFront.getLength()) )){
//            logger.debug("net distance with respect to leader from new roadId. addRoadlength={}", roadLength);
//            netGap += roadLength;
//        }            
//        return netGap;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see
//     * org.movsim.simulator.vehicles.Vehicle#relSpeed(org.movsim.simulator.vehicles
//     * .Vehicle)
//     */
//    @Override
//    public double getRelSpeed(Moveable vehFront) {
//        if (vehFront == null)
//            return 0;
//        return (speed - vehFront.getSpeed());
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#calcAcceleration(double,
//     * org.movsim.simulator.vehicles.VehicleContainer, double, double)
//     */
//    @Override
//    public void calcAcceleration(double dt, final VehicleContainer vehContainer,
//            final VehicleContainer vehContainerLeftLane, double alphaT, double alphaV0) {
//
//        accOld = acc;
//        // acceleration noise:
//        double accError = 0;
//        if (noise != null) {
//            noise.update(dt);
//            accError = noise.getAccError();
//            final Moveable vehFront = vehContainer.getLeader(this);
//            if (getNetDistance(vehFront) < Constants.CRITICAL_GAP) {
//                accError = Math.min(accError, 0.); // !!!
//            }
//            // logger.debug("accError = {}", accError);
//        }
//
//        // TODO extract to super class
//        double alphaTLocal = alphaT;
//        double alphaV0Local = alphaV0;
//        double alphaALocal = 1;
//
//        // TODO check concept here: combination with alphaV0 (consideration of
//        // reference v0 instead of dynamic v0 which depends on speedlimits)
//        if (memory != null) {
//            final double v0 = accelerationModel.getDesiredSpeedParameterV0();
//            memory.update(dt, speed, v0);
//            alphaTLocal *= memory.alphaT();
//            alphaV0Local *= memory.alphaV0();
//            alphaALocal *= memory.alphaA();
//        }
//
//        accModel = calcAccModel(vehContainer, vehContainerLeftLane, alphaTLocal, alphaV0Local, alphaALocal);
//
//        // consider red or amber/yellow traffic light:
//        if (trafficLightApproaching.considerTrafficLight()) {
//            acc = Math.min(accModel, trafficLightApproaching.accApproaching());
//            // logger.debug("accModel = {}, accTrafficLight = {}", accModel,
//            // accTrafficLight );
//        } else {
//            acc = accModel;
//        }
//
//        acc = Math.max(acc + accError, -maxDecel); // limited to maximum
//                                                   // deceleration
//        // logger.debug("acc = {}", acc );
//    }
//
//    // TODO this acceleration is the base for MOBIL decision: could consider
//    // also noise (for transfering stochasticity to lane-changing) and other
//    // relevant traffic situations!
//    //
//    @Override
//    public double calcAccModel(final VehicleContainer vehContainer, final VehicleContainer vehContainerLeftLane) {
//        return calcAccModel(vehContainer, vehContainerLeftLane, 1, 1, 1);
//    }
//
//    private double calcAccModel(final VehicleContainer vehContainer, final VehicleContainer vehContainerLeftLane,
//            double alphaTLocal, double alphaV0Local, double alphaALocal) {
//
//        double acc;
//
//        if (lcModel.isInitialized() && lcModel.withEuropeanRules()) {
//            acc = accelerationModel.calcAccEur(lcModel.vCritEurRules(), this, vehContainer, vehContainerLeftLane,
//                    alphaTLocal, alphaV0Local, alphaALocal);
//        } else {
//            acc = accelerationModel.calcAcc(this, vehContainer, alphaTLocal, alphaV0Local, alphaALocal);
//        }
//
//        return acc;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#updatePostionAndSpeed(double)
//     */
//    @Override
//    public void updatePostionAndSpeed(double dt) {
//
//        // logger.debug("dt = {}", dt);
//        // first increment postion, 
//        // then increment s with *new* v (second order: -0.5 a dt^2)
//
//        positionOld = position;
//
//        if (accelerationModel.isCA()) {
//            speed = (int) (speed + dt * acc + 0.5);
//            position = (int) (position + dt * speed + 0.5);
//
//        } else {
//            // continuous microscopic models and iterated maps
//            if (speed < 0) {
//                speed = 0;
//            }
//            final double advance = (acc * dt >= -speed) ? speed * dt + 0.5 * acc * dt * dt : -0.5 * speed * speed / acc;
//
//            position += advance;
//            speed += dt * acc;
//            if (speed < 0) {
//                speed = 0;
//                acc = 0;
//            }
//
//        }
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#getLane()
//     */
//    @Override
//    public int getLane() {
//        return lane;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#hasReactionTime()
//     */
//    @Override
//    public boolean hasReactionTime() {
//        return (reactionTime + Constants.SMALL_VALUE > 0);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#updateTrafficLight(double,
//     * org.movsim.simulator.roadSection.TrafficLight)
//     */
//    @Override
//    public void updateTrafficLight(double time, TrafficLight trafficLight) {
//        trafficLightApproaching.update(this, time, trafficLight, accelerationModel);
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.movsim.simulator.vehicles.Vehicle#removeObservers()
//     */
//    @Override
//    public void removeObservers() {
//        accelerationModel.removeObserver();
//    }
//
//    @Override
//    public LaneChangingModelImpl getLaneChangingModel() {
//        return lcModel;
//    }
//
//    /* (non-Javadoc)
//     * @see org.movsim.simulator.vehicles.Vehicle#getAccelerationModel()
//     */
//    @Override
//    public AccelerationModel getAccelerationModel() {
//        return accelerationModel;
//    }
//
//    // ---------------------------------------------------------------------------------
//    // lane-changing related methods
//    // ---------------------------------------------------------------------------------
//
//    /* (non-Javadoc)
//     * @see org.movsim.simulator.vehicles.Vehicle#considerLaneChanging(double, java.util.List)
//     */
//    @Override
//    public boolean considerLaneChanging(double dt, final List<VehicleContainer> vehContainers) {
//        // no lane changing when not configured in xml.
//        if (!lcModel.isInitialized()) {
//            return false;
//        }
//
//        if (inProcessOfLaneChanging()) {
//            updateLaneChangingDelay(dt);
//            return false;
//        }
//
//        // no lane-changing decision necessary for one-lane road
//        if (vehContainers.size() < 2) {
//            return false;
//        }
//
//        
//        final int saveLane = lane;
//        // if not in lane-changing process do determine if new lane is more
//        // attractive and lane change is possible
//        final int laneChangingDirection = lcModel.determineLaneChangingDirection(vehContainers);
//
//        // TODO do cross check, not necessary anymore?! 
//        //assert saveLane != lane;
//
//        // initiates a lane change: set targetLane to new value
//        // the lane will be assigned by the vehicle container !!
//        if (laneChangingDirection != Constants.NO_CHANGE) {
//            setTargetLane(lane + laneChangingDirection);
//            resetDelay();
//            updateLaneChangingDelay(dt);
//            logger.debug("do lane change to={} into target lane={}", laneChangingDirection, targetLane);
//            return true;
//        }
//
//        return false;
//    }
//
//    @Override
//    public void initLaneChangeFromRamp(int oldLane) {
//        laneOld = oldLane; // Constants.MOST_RIGHT_LANE + Constants.TO_RIGHT; //
//                           // virtual lane index from onramp
//        resetDelay();
//        final double delayInit = 0.2; // needs only to be > 0;
//        updateLaneChangingDelay(delayInit);
//        logger.debug("do lane change from ramp: virtual old lane (origin)={}, contLane={}", lane, getContinousLane());
//        if (oldLane == Constants.TO_LEFT) {
////            System.out.printf(".......... do lane change from ramp: virtual old lane (origin)=%d, contLane=%.4f", lane,
////                    getContinousLane());
//            logger.debug("do lane change from ramp: virtual old lane (origin)={}, contLane={}", lane,
//                    getContinousLane());
//        }
//    }
//
//    /* (non-Javadoc)
//     * @see org.movsim.simulator.vehicles.Vehicle#getTargetLane()
//     */
//    @Override
//    public int getTargetLane() {
//        return targetLane;
//    }
//
//    /**
//     * Sets the target lane.
//     *
//     * @param targetLane the new target lane
//     */
//    private void setTargetLane(int targetLane) {
//        assert targetLane >= 0;
//        this.targetLane = targetLane;
//    }
//
//    /* (non-Javadoc)
//     * @see org.movsim.simulator.vehicles.Vehicle#inProcessOfLaneChanging()
//     */
//    public boolean inProcessOfLaneChanging() {
//        return (tLaneChangingDelay > 0 && tLaneChangingDelay < FINITE_LANE_CHANGE_TIME_S);
//    }
//
//    /**
//     * Reset delay.
//     */
//    private void resetDelay() {
//        tLaneChangingDelay = 0;
//    }
//
//    /**
//     * Update lane changing delay.
//     *
//     * @param dt the dt
//     */
//    public void updateLaneChangingDelay(double dt) {
//        tLaneChangingDelay += dt;
//    }
//
//    /* (non-Javadoc)
//     * @see org.movsim.simulator.vehicles.Moveable#getContinousLane()
//     */
//    @Override
//    public double getContinousLane() {
//        if (inProcessOfLaneChanging()) {
//            final double fractionTimeLaneChange = Math.min(1, tLaneChangingDelay / FINITE_LANE_CHANGE_TIME_S);
//            return fractionTimeLaneChange * lane + (1 - fractionTimeLaneChange) * laneOld;
//        }
//        return getLane();
//    }
//
//    // ---------------------------------------------------------------------------------
//    // braking lights for neat viewers
//    // ---------------------------------------------------------------------------------
//
//    /* (non-Javadoc)
//     * @see org.movsim.simulator.vehicles.Moveable#isBrakeLightOn()
//     */
//    @Override
//    public boolean isBrakeLightOn() {
//        updateBrakeLightStatus();
//        return isBrakeLightOn;
//    }
//
//    /**
//     * Update brake light status.
//     */
//    private void updateBrakeLightStatus() {
//        if (isBrakeLightOn) {
//            if (acc > -THRESHOLD_BRAKELIGHT_OFF || speed <= 0.0001) {
//                isBrakeLightOn = false;
//            }
//        } else if (accOld > -THRESHOLD_BRAKELIGHT_ON && acc < -THRESHOLD_BRAKELIGHT_ON) {
//            isBrakeLightOn = true;
//        }
//    }
//
//    // ---------------------------------------------------------------------------------
//    // converter for scaled quantities in cellular automata
//    // ---------------------------------------------------------------------------------
//
//    @Override
//    public PhysicalQuantities physicalQuantities() {
//        return physQuantities;
//    }
//    
//    
//    @Override
//    public double getActualFuelFlowLiterPerS(){
//        if(fuelModel==null){
//            return 0;
//        }
//        return fuelModel.getFuelFlowInLiterPerS(speed, acc);
//    }
//    // Added as part of xodr merge
//    /**
//     * 'Not Set' road exit position value, guaranteed not to be used by any vehicles.
//     */
//    public static final double EXIT_POSITION_NOT_SET = -1.0;
//
//    /**
//     * Vehicle type.
//     */
//    public static enum Type {
//        /**
//         * Vehicle type has not been set.
//         */
//        NONE,
//        /**
//         * Vehicle is an immovable obstacle.
//         */
//        OBSTACLE,
//        /**
//         * Vehicle is nominally a car.
//         */
//        CAR,
//        /**
//         * Vehicle is nominally a truck.
//         */
//        TRUCK,
//        /**
//         * The vehicle is a test car, used to gather data about traffic conditions.
//         */
//        TEST_CAR
//    }
//    /**
//     * Returns this vehicle's type.
//     * 
//     * @return vehicle's type
//     * 
//     */
//    public final Vehicle.Type type() {
//        return type;
//    }
//    /**
//     * Sets this vehicle's type.
//     * @param type 
//     * 
//     */
//    public final void setType(Vehicle.Type type) {
//        this.type = type;
//    }
//
//}
