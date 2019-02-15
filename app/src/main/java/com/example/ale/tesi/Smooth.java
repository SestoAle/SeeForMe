package com.example.ale.tesi;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Ale on 16/07/16.
 */

public class Smooth {

    /*
        Algoritmo di smoothing utile per rendere più "morbido" lo spostamento delle icone del
        riconoscimento, che altrimenti risulterebbe troppo macchinoso. L'algoritmo è basato su una
        media pesata esponenziale: l'attributo span fa riferimento a quanti valori precedenti vengono presi
        in considerazione per calcolare la media. Il peso di ogni elemento è dato dall'attributo e
        elevato alla posizione del valore. Gestendo questi 2 valori è possibile aumentare lo
        smoothing, ma essendo basato solamente su elementi precedenti nel tempo, più si aumenta lo
        smoothing, più è evidente un "effetto ritardo". Entrambi i valori sono settati ad un valore
        che è sembrato ottimale per lo scopo.
     */

    private static int span = 10;
    private static final double e = 1.2;
    private static ArrayList<ArrayList> more_vals_left = new ArrayList<ArrayList>();
    private static ArrayList<ArrayList> more_vals_top = new ArrayList<ArrayList>();
    private static ArrayList<ArrayList> more_vals_right = new ArrayList<ArrayList>();
    private static ArrayList<Integer> vals_left = new ArrayList<Integer>();
    private static ArrayList<Integer> vals_top = new ArrayList<Integer>();
    private static ArrayList<Integer> vals_right = new ArrayList<Integer>();


    static public int smooth_left(int val, int index){

        if(more_vals_left.size() < index + 1){
            more_vals_left.add(index, new ArrayList<Integer>());
        }

        vals_left = more_vals_left.get(index);

        double smooth = 0;

        if(vals_left.size() == 0){
            vals_left.add(val);
        }
        if (Math.abs(val - vals_left.get(vals_left.size() - 1)) > 100){
            more_vals_left.set(index, new ArrayList<Integer>());
            vals_left = more_vals_left.get(index);
            vals_left.add(val);
        } else {
            if(vals_left.size() == span){
                vals_left.remove(0);
                vals_left.add(val);
            } else {
                vals_left.add(val);
            }
        }

        double sum_weight = 0;

        /* for (int i: vals_left){
            smooth += i;
        } */

        /* for (int i = 0; i < vals_left.size(); i++) {
            smooth += vals_left.get(i) * (i + 1)*(i + 1);
            sum_weight += (i + 1)*(i + 1);
        } */

        for (int i = 0; i < vals_left.size(); i++) {
            double weight = Math.pow(e, -(vals_left.size() - 1 - i));
            smooth += vals_left.get(i) * weight;
            sum_weight += weight;
        }

        smooth = smooth/sum_weight;

        more_vals_left.set(index, vals_left);

        return (int)smooth;
    }

    static public int smooth_top(int val, int index){

        if(more_vals_top.size() < index + 1){
            more_vals_top.add(index, new ArrayList<Integer>());
        }

        vals_top = more_vals_top.get(index);

        double smooth = 0;

        if(vals_top.size() == 0){
            vals_top.add(val);
        }
        if (Math.abs(val - vals_top.get(vals_top.size() - 1)) > 100){
            more_vals_top.set(index, new ArrayList<Integer>());
            vals_top = more_vals_top.get(index);
            vals_top.add(val);
        } else {
            if(vals_top.size() == span){
                vals_top.remove(0);
                vals_top.add(val);
            } else {
                vals_top.add(val);
            }
        }

        double sum_weight = 0;

        /* for (int i: vals_left){
            smooth += i;
        } */

        /* for (int i = 0; i < vals_left.size(); i++) {
            smooth += vals_left.get(i) * (i + 1)*(i + 1);
            sum_weight += (i + 1)*(i + 1);
        } */

        for (int i = 0; i < vals_top.size(); i++) {
            double weight = Math.pow(e, -(vals_top.size() - 1 - i));
            smooth += vals_top.get(i) * weight;
            sum_weight += weight;
        }

        smooth = smooth/sum_weight;

        more_vals_top.set(index, vals_top);

        return (int)smooth;
    }

    static public int smooth_right(int val, int index){

        if(more_vals_right.size() < index + 1){
            more_vals_right.add(index, new ArrayList<Integer>());
        }

        vals_right = more_vals_right.get(index);

        double smooth = 0;

        if(vals_right.size() == 0){
            vals_right.add(val);
        }
        if (Math.abs(val - vals_right.get(vals_right.size() - 1)) > 100){
            more_vals_right.set(index, new ArrayList<Integer>());
            vals_right = more_vals_right.get(index);
            vals_right.add(val);
        } else {
            if(vals_right.size() == span){
                vals_right.remove(0);
                vals_right.add(val);
            } else {
                vals_right.add(val);
            }
        }

        double sum_weight = 0;

        /* for (int i: vals_left){
            smooth += i;
        } */

        /* for (int i = 0; i < vals_left.size(); i++) {
            smooth += vals_left.get(i) * (i + 1)*(i + 1);
            sum_weight += (i + 1)*(i + 1);
        } */

        for (int i = 0; i < vals_right.size(); i++) {
            double weight = Math.pow(e, -(vals_right.size() - 1 - i));
            smooth += vals_right.get(i) * weight;
            sum_weight += weight;
        }

        smooth = smooth/sum_weight;

        more_vals_right.set(index, vals_right);

        return (int)smooth;
    }


    /* static public int smooth_top(int val, int size){

        if(size != size_top){
            size_top = size;
            more_vals_top.clear();
            for(int i = 0; i < size_top; i++){
                more_vals_top.add(new ArrayList<Integer>());
            }
            size_top_count = 0;
        }

        vals_top = more_vals_top.get(size_top_count);

        if(vals_top.size() == 0){
            vals_top.add(val);
        }
        if (Math.abs(val - vals_top.get(vals_top.size() - 1)) > 100){
            more_vals_top.remove(size_top_count);
            more_vals_top.add(size_top_count, new ArrayList<Integer>());
            vals_top = more_vals_top.get(size_top_count);
            vals_top.add(val);
        }

        int smooth = 0;

        for (int i: vals_top) {
            smooth += i;
        }

        smooth = smooth/vals_top.size();

        if(vals_top.size() == span){
            vals_top.remove(0);
            vals_top.add(val);
        } else {
            vals_top.add(val);
        }

        if(size > 1){
            size_top_count ++;
        }
        if(size_top_count >= size)
        {
            size_top_count = 0;
        }

        return smooth;
    }


    static public int smooth_left(int val){


        if(vals_left.size() == 0){
            vals_left.add(val);
        }
        if (Math.abs(val - vals_left.get(vals_left.size() - 1)) > 100){
            vals_left = new ArrayList<Integer>();
            vals_left.add(val);
        }

        int smooth = 0;

        for (int i: vals_left) {
            smooth += i;
        }

        smooth = smooth/vals_left.size();

        if(vals_left.size() == span){
            vals_left.remove(0);
            vals_left.add(val);
        } else {
            vals_left.add(val);
        }

        return smooth;
    }

    static public int smooth_right(int val){


        if(vals_right.size() == 0){
            vals_right.add(val);
        }
        if (Math.abs(val - vals_right.get(vals_right.size() - 1)) > 100){
            vals_right = new ArrayList<Integer>();
            vals_right.add(val);
        }

        int smooth = 0;

        for (int i: vals_right) {
            smooth += i;
        }

        smooth = smooth/vals_right.size();

        if(vals_right.size() == span){
            vals_right.remove(0);
            vals_right.add(val);
        } else {
            vals_right.add(val);
        }

        return smooth;
    }

    static public int smooth_top(int val){


        if(vals_top.size() == 0){
            vals_top.add(val);
        }
        if (Math.abs(val - vals_top.get(vals_top.size() - 1)) > 100){
            vals_top = new ArrayList<Integer>();
            vals_top.add(val);
        }

        int smooth = 0;

        for (int i: vals_top) {
            smooth += i;
        }

        smooth = smooth/vals_top.size();

        if(vals_top.size() == span){
            vals_top.remove(0);
            vals_top.add(val);
        } else {
            vals_top.add(val);
        }

        return smooth;
    } */

}
