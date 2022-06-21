package com.sequential;

import java.io.*;
import java.util.Vector;

import java.util.Scanner;

public class Matrix {

    // N x M
    // y0x0 y0x1 y0x2 ... y0xM
    // y1x0 y1x1 y1x2 ... y1xM
    // ...
    // yNx0 yNx1 yNx2 ... yNxM

    private int[] matrix;
    private int n;
    private int m;

    public Matrix(int n, int m) {
        this.n = n;
        this.m = m;
        this.matrix = new int[n * m];
    }
    public Matrix(int[] matrix, int rowLength) {
        this.matrix = matrix;
        this.n = matrix.length / rowLength;
        this.m = rowLength;
    }

    @Override
    public String toString() {
        String s = "";
        s += this.n + "x" + this.m + "\n";

        for (int i = 0; i < this.n; i++) {
            for (int j = 0; j < this.m; j++)
                s += this.matrix[i * this.m + j] + " ";

            s += "\n";
        }
        return s;
    }

    // x - cols, y - rows
    public int getCell(int x, int y) {
        return this.matrix[y * this.m + x];
    }

    public int[] getArrayMatrix(){
        return matrix;
    }

    public void setCell(int x, int y, int value) {
        this.matrix[y * this.m + x] = value;
    }

    public Matrix multiply(Matrix B) {

        if (this.m != B.n)
            throw new IllegalArgumentException();

        int[] matrixA = this.matrix;
        int[] matrixB = B.matrix;

        int[] resultMat = new int[this.n *B.m];

        for (int k = 0; k < this.n; k++)
            for(int j = 0; j < B.m; j++) {
                int sum = 0;

                for (int i = 0; i < B.n; i++)
                    sum += matrixB[i*B.m + j] * matrixA[k * this.m + i];
                resultMat[k * B.m + j] = sum;
    }

        return new Matrix(resultMat, B.m);
    }

    public void fillRandoms() {
        for(int j = 0; j < this.n; j++)
            for(int i = 0; i < this.m; i++)
                this.matrix[j * this.m + i] = ((int)Math.round(Math.random() * Short.MAX_VALUE / 10));
    }

    public void writeFile(String name) {
        String path = "./" + name + ".mat";
        File file = new File(path);
        if(file.exists())
            file.delete();
        try {
            file.createNewFile();
            FileWriter writer = new FileWriter(path);
            //BufferedWriter bWriter = new BufferedWriter(writer, 32768);
            writer.write(this.n + "x" + this.m + "\n");
            for (int i = 0; i< this.n; i++){
                for (int j = 0; j < this.m; j++)
                    writer.write(this.matrix[i*this.m + j] + " ");
                writer.write("\n");
            }
            //writer.write(toString());
            writer.close();
        }
        catch (IOException err){
            System.out.println("IOError (writing) occurred...");
        }
    }

    public static Matrix readFile(String name) {
        String path = "./" + name + ".mat";
        int[] matrix = null;
        int n, m = 0;

        File file = new File(path);
        try {
            if (!file.exists())
                throw new IOException();

            Scanner reader = new Scanner(file);
            String row = reader.nextLine();

            String[] dims = row.split("x");
            n = Integer.parseInt(dims[0]);
            m = Integer.parseInt(dims[1]);
            matrix = new int[n * m];
            int c = 0;
            while(reader.hasNext()) {
                row = reader.nextLine();
                row = row.substring(0, row.length() - 1);
                String[] values = row.split(" ");
                for (int j = 0; j < values.length; j++)
                    matrix[c * m + j] = Integer.parseInt(values[j]);
                c++;
            }
            reader.close();
        }
        catch (IOException err){
            System.out.println("IOError (reading) occurred...");
        }
        return new Matrix(matrix, m);
    }

    public static void main(String[] args) {
        boolean flag = true;
        if (flag){
            Scanner sc= new Scanner(System.in);
            System.out.print("Enter m - ");
            int m= sc.nextInt();
            System.out.print("Enter n - ");
            int n= sc.nextInt();
            System.out.print("Enter q - ");
            int q= sc.nextInt();
            String name1 = m + "x" + n;
            String name2 = n + "x" + q;
            String path1 = "./" + m + "x" + n + "A.mat";
            String path2 = "./" + n + "x" + q + "B.mat";
            Matrix mat1 = new Matrix(m, n);
            Matrix mat2 = new Matrix(n, q);
            File file = new File(path1);
            if (!file.exists()) {
                mat1.fillRandoms();
                mat1.writeFile(name1 + "A");
                System.out.println("Matrix A");
                System.out.println(mat1);
            }
            file = new File(path2);
            if (!file.exists()) {
                mat2.fillRandoms();
                mat2.writeFile(name2 + "B");
                System.out.println("Matrix B");
                System.out.println(mat2);
            }
            System.out.println("Matrix C");
            Matrix mat3 = mat1.multiply(mat2);
            System.out.println(mat3);


        }else {
            long start = 0,
                    timestamp = 0;
            for (int i = 1; i <= 8; i++) {
                for (int j = 0; j < 5; j++) {
                    int size = i * 200;
                    String name = size + "x" + size;
                    String path1 = "./" + name + "A.mat";
                    String path2 = "./" + name + "B.mat";
                    Matrix mat1 = new Matrix(size, size);
                    Matrix mat2 = new Matrix(size, size);
                    File file = new File(path1);
                    if (!file.exists()) {
                        mat1.fillRandoms();
                        mat1.writeFile(name + "A");
                    }
                    file = new File(path2);
                    if (!file.exists()) {
                        mat2.fillRandoms();
                        mat2.writeFile(name + "B");
                    }

                    start = System.nanoTime();
                    mat1.multiply(mat2);
                    timestamp += System.nanoTime() - start;

                }
                System.out.println(timestamp / 5 * 0.000001);
                start = timestamp = 0;
            }
        }
    }
}
