package ipqm.lafiaa.algoritmogenetico.domain.dto;

import ipqm.lafiaa.algoritmogenetico.domain.rvt.Buoy;

public class BuoyOutputDTO {
    private int x;
    private int y;
    private String name;

    public BuoyOutputDTO(){}

    public BuoyOutputDTO(Buoy buoy){
        this.x = buoy.getPosX();
        this.y = buoy.getPosY();
        this.name = buoy.getNome();
    }

    public BuoyOutputDTO(int x, int y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "{ x:" + x +
                ", y:" + y +
                ", name:'" + name + '}';
    }
}
