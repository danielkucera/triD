package eu.danman.trid;

/**
 * Created by dano on 9/10/14.
 */
public class triPoint {

    private double x;
    private double y;
    private double z;

    public triPoint(double _x, double _y, double _z){
        x = _x;
        y = _y;
        z = _z;
    }

    double getX(){
        return x;
    }

    double getY(){
        return y;
    }

    double getZ(){
        return z;
    }
}
