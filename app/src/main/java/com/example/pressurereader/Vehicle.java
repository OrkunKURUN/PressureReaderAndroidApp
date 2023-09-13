package com.example.pressurereader;

public class Vehicle {
    public String plate, id;
    public int vehicleType; //0: Semi, 1: Trailer
    public boolean isLeaking;

    public Vehicle(){}
    public Vehicle(String plate, String id, int vehicleType, boolean isLeaking){
        this.plate = plate;
        this.id = id;
        this.vehicleType = vehicleType;
        this.isLeaking = isLeaking;
    }

    public int getVehicleType() {
        return vehicleType;
    }

    public String getId() {
        return id;
    }

    public String getPlate() {
        return plate;
    }

    public boolean getLeaking() {
        return isLeaking;
    }

    public void setVehicleType(int vehicleType) {
        this.vehicleType = vehicleType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public void setLeaking(boolean leaking) {
        isLeaking = leaking;
    }
}
