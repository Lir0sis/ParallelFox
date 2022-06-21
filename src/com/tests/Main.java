package com.tests;

import com.sequential.Matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) {
        //Matrix mat = new Matrix(2,3);
        //mat.fillRandoms();
        //mat.writeFile("2x3");
        //Matrix mat2 = Matrix.readFile("2x3");
        //System.out.println(mat2);

        //Matrix mat3 = new Matrix(3, 5);
        //mat3.fillRandoms();
        //System.out.println(mat3);
        //mat = mat2.multiply(mat3);
        //System.out.println(mat);

        for (int i = 15; i <= 15; i++) {
            int size = 20;//i * 96;
            String name = size + "x" + size;
            String path1 = "./" + name + "A.mat";
            String path2 = "./" + name + "B.mat";
            Matrix mat = new Matrix(size, size);
            File file = new File(path1);
            if (!file.exists()) {
                mat.fillRandoms();
                mat.writeFile( name + "A");
            }
            file = new File(path2);
            if (!file.exists()) {
                mat.fillRandoms();
                mat.writeFile(name + "B");
            }
        }
    }
}
