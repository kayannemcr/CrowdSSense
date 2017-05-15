package com.example.lenovo.pmcrowdssensev1;


public class Messages {
    private String _realtime;
    private String _PMV;
    private String _coordinates;
    private String _deviceID;



    //Added this empty constructor in lesson 50 in case we ever want to create the object and assign it later.
    public Messages(){
    }
    // public Messages(String messageName,String_PMV,String CO, String NO2, String VOC, String TEMP, String RH, String BATT, long millis, String realtime, String coordinates, String longi, String lat) {
    public Messages(String realtime, String PMV, String coordinates, String deviceID) {
        //public Messages(String messageName, long millis, String realtime, String coordinates) {
        this._PMV = PMV;
        this._realtime = realtime;
        this._coordinates = coordinates;
        this._deviceID = deviceID;
    }

    public String get_PMV() {
        return _PMV;
    }

    public void set_PMV(String _PMV) {
        this._PMV = _PMV;
    }


    public String get_realtime() {
        return _realtime;
    }

    public void set_realtime(String _realtime) {
        this._realtime = _realtime;
    }

    public String get_coordinates() {
        return _coordinates;
    }

    public void set_coordinates(String _coordinates) {
        this._coordinates = _coordinates;
    }


    public String get_deviceID() {
        return _deviceID;
    }

    public void set_deviceID(String _deviceID) {
        this._deviceID = _deviceID;
    }


}
