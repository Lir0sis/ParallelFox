package com.parallel;
import com.sequential.Matrix;
import mpi.*;

import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class MyMPI {
    static int ProcNum = 0; // Number of available processes
    static int ProcRank = 0; // Rank of current process
    static int Gridsize; // size of virtual processor grid
    static int[] GridCoords = new int[2]; // Coordinates of current processor in grid
    static Cartcomm GridComm; // Grid communicator
    static Cartcomm ColComm; // Column communicator
    static Cartcomm RowComm; // Row communicator
    static PrintStream out;

    public static void DummyDataInitialization(int[][] aMatrix, int[][] bMatrix, int size) {
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {
                aMatrix[i][j] = 1;
                bMatrix[i][j] = 1;
            }
    }
    public static void DummyDataInitialization(int[] aMatrix, int[] bMatrix, int size) {
        for (int i = 0; i < size; i++)
            for( int j = 0; j < size; j++){
                aMatrix[i*size + j] = i + 1 + j*i;
                bMatrix[i*size + j] = i*j*2 + 1 + i;
            }
    }

    // Виведення матриць
    public static void PrintMatrix(int[] pMatrix, int RowCount, int ColCount) {
        int i, j; // Loop variables
        for (i = 0; i < RowCount; i++) {
            for (j = 0; j < ColCount; j++)
                System.out.print(" " + pMatrix[i * ColCount + j]);
            System.out.print("\n");
        }
    }

    public static int[] matToArray(int[][] matrix) {
        int[] arr = new int[matrix.length * matrix[0].length];
        for(int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix[0].length; j++)
                arr[i*matrix[0].length + j] = matrix[i][j];
        return arr;
    }

    public static int[][] arrToMat(int[] arr, int rowLength){
        int colLength = arr.length/rowLength;
        int[][] mat = new int[colLength][rowLength];
        for(int i = 0; i < colLength; i++)
            for (int j = 0; j < rowLength; j++)
                mat[i][j] = arr[i*rowLength + j];
        return mat;
    }

    public static void copyMatrix(int[][] dest, int[][] src) {
        if (dest.length == src.length && dest[0].length == src[0].length)
            for (int i = 0; i < dest.length; i++)
                for (int j = 0; j < dest[0].length; j++)
                    dest[i][j] = src[i][j];
        else throw new IllegalArgumentException();
    }

    public static void copyArray(int[] dest, int[] src){
        for(int i = 0; i < dest.length; i++)
            dest[i] = src[i];
    }

    public static void fillZeros(int[][] dest) {
        for (int i = 0; i < dest.length; i++)
            for (int j = 0; j < dest[0].length; j++)
                dest[i][j] = 0;
    }

    public static void fillZeros(int[] dest) {
        for (int i = 0; i < dest.length; i++)
                dest[i] = 0;
    }

    // Function for matrix multiplication
    public static void SerialResultCalculation(int[] aMatrix, int[] bMatrix, int[] cMatrix, int size) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++)
                for (int k = 0; k < size; k++)
                    cMatrix[i*size + j] += aMatrix[i * size + k] * bMatrix[k * size + j];
        }
    }

    // Створення комунікаторів решітки, рядків, стовбців
    public static void CreateGridCommunicators() {
        // Створення декартової топології, комунікатора решітки
        GridComm = MPI.COMM_WORLD.Create_cart(new int[]{Gridsize, Gridsize}, new boolean[] {false, false}, true);

        ProcRank = GridComm.Rank();
        GridCoords = GridComm.Coords(ProcRank);
        RowComm = GridComm.Sub(new boolean[]{false, true});
        ColComm = GridComm.Sub(new boolean[]{true, false});
    }

    // Function for checkerboard matrix decomposition
    public static void CheckerboardMatrixScatter(int[] pMatrix, int[] pMatrixBlock, int size, int blocksize) {
        int[] MatrixRow = new int[blocksize * size];
        if (GridCoords[1] == 0) {
            ColComm.Scatter(pMatrix, 0, blocksize * size, MPI.INT, MatrixRow,
                    0, blocksize * size, MPI.INT, 0);
        }
        int [][] matMat = arrToMat(MatrixRow, size);

        int [][] matBlock = new int[blocksize][blocksize];

        for (int i = 0; i < blocksize; i++) {
            RowComm.Scatter(matMat[i], 0, blocksize, MPI.INT,
                    matBlock[i], 0, blocksize, MPI.INT, 0);
        }
        copyArray(pMatrixBlock, matToArray(matBlock));
    }

    // Розсилання даних по процесах
    public static void DataDistribution(int[] aMatrix, int[] bMatrix, int[]
            pTempAblock, int[] pBblock, int size, int blocksize) {
        CheckerboardMatrixScatter(aMatrix, pTempAblock, size, blocksize);
        CheckerboardMatrixScatter(bMatrix, pBblock, size, blocksize);
    }

    // Збір даних до початкового процесу
    public static void ResultCollection(int[] cMatrix, int[] pCblock, int size, int blocksize) {
        int[][] pResultRow = new int[blocksize][size];

        for (int i = 0; i < blocksize; i++)
            RowComm.Gather( pCblock,i * blocksize, blocksize, MPI.INT,
                    pResultRow[i],0, blocksize, MPI.INT, 0);

        if (GridCoords[1] == 0)
            ColComm.Gather(matToArray(pResultRow), 0,blocksize * size, MPI.INT, cMatrix,
                    0, blocksize * size, MPI.INT, 0);
    }

    public static void ParallelResultCalculation(int[] pAblock, int[] pTempAblock,
                                   int[] pBblock, int[] pCblock, int blocksize) {
        for (int iter = 0; iter < Gridsize; iter++) {
            int bcastRoot = (GridCoords[0] + iter) % Gridsize;
            if (GridCoords[1] == bcastRoot)
                copyArray(pAblock, pTempAblock);
            // Розсилка даних матриці А
            RowComm.Bcast(pAblock, 0,blocksize * blocksize, MPI.INT, bcastRoot);

            // Обчислення блоків
            for (int i = 0; i < blocksize; i++)
                for (int j = 0; j < blocksize; j++)
                    for (int k = 0; k < blocksize; k++)
                        pCblock[i*blocksize + j] += pAblock[i * blocksize + k]
                                * pBblock[k * blocksize + j];

                    //TestBlocks(pCblock, blocksize, "cBlocks");

            // Зміщення елементів В
            int NextProc = GridCoords[0] + 1;
            if ( GridCoords[0] == Gridsize-1 ) NextProc = 0;
            int PrevProc = GridCoords[0] - 1;
            if ( GridCoords[0] == 0 ) PrevProc = Gridsize-1;

            ColComm.Sendrecv_replace(pBblock, 0, blocksize * blocksize, MPI.INT,
                    NextProc, 0, PrevProc, 0);
        }
    }

    // Вивід блоків на екран
    public static void TestBlocks(int[] pBlock, int blocksize, String mess) {
        MPI.COMM_WORLD.Barrier();
        if (ProcRank == 0) {
            System.out.println(mess);
        }
        for (int i = 0; i < ProcNum; i++) {
            if (ProcRank == i) {
                System.out.println("ProcRank = " + ProcRank + "\n");
                PrintMatrix(pBlock, blocksize, blocksize);
            }
            MPI.COMM_WORLD.Barrier();
        }
    }

    // Перевірка правильності обчислень
    public static void TestResult(int[] aMatrix, int[] bMatrix, int[] cMatrix,
                    int size) {
        int[] pSerialResult;
        double Accuracy = 1.e-6;
        int equal = 0;
        pSerialResult = new int[size * size];
        fillZeros(pSerialResult);
        SerialResultCalculation(aMatrix, bMatrix, pSerialResult, size);
        for (int i = 0; i < size * size; i++)
            if (Math.abs(pSerialResult[i] - cMatrix[i]) >= Accuracy)
                equal = 1;

        if (equal == 1)
            System.out.println("Матриці не однакові.");
        else
            System.out.println("Матриці однакові.");
        PrintMatrix(pSerialResult, size, size);
        out.println("----");
        PrintMatrix(cMatrix, size, size);
    }

    public static int[] loadMatrix(int size, String name) {
        String path = "./" + name + ".mat";
        Matrix mat = new Matrix(size, size);

        File file = new File(path);
        if (!file.exists()) {
            mat.fillRandoms();
            mat.writeFile(name);
        }
        else
            mat = Matrix.readFile(name);

        return mat.getArrayMatrix();

    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        int[] aMatrix = new int[1],
                bMatrix = new int[1],
                cMatrix = new int[1],
                pAblock = null,
                pBblock = null,
                pCblock = null,
                pTempAblock = null,
                buffer = new int[1];
        int size = 0, blocksize = 0;
        int loopsCount = 3;

        out = new PrintStream(System.out, true, "UTF-8");

        MPI.Init(args);

        ProcNum = MPI.COMM_WORLD.Size();
        ProcRank = MPI.COMM_WORLD.Rank();
        Gridsize = (int) Math.sqrt(ProcNum);

        CreateGridCommunicators();

        if (ProcRank == 0) {
            if (ProcNum != Gridsize * Gridsize) {
                out.println("Неправильная кількість процессів. Має бути можливим корінь з ProcNum");
                MPI.Finalize();
                return;
            }
        }
        long timeStampSeq = 0,
            timeStampPar = 0;
        long startTime = 0;
        for (int i = 1; i <= 8; i++) {
            if (ProcRank == 0) {
                buffer[0] = size = i * 200;
                if (size % Gridsize != 0) {
                    out.println("Неможливо поділити матрицю між процесами!");
                    MPI.Finalize();
                    return;
                }
            }
            for (int j = 0; j < loopsCount; j++) {

            GridComm.Bcast(buffer, 0, 1, MPI.INT, 0);
            size = buffer[0];
            aMatrix = new int[size * size];
            bMatrix = new int[size * size];
            cMatrix = new int[size * size];
            if (ProcRank == 0) {
                aMatrix = loadMatrix(size, size + "x" + size + "A");
                bMatrix = loadMatrix(size, size + "x" + size + "B");
                //PrintMatrix(aMatrix, size, size);
                //out.println(" -- -- ");
                //PrintMatrix(bMatrix, size, size);

                startTime = System.nanoTime();
            }

            blocksize = size / Gridsize;
            pAblock = new int[blocksize * blocksize]; // mat Ablock
            pBblock = new int[blocksize * blocksize]; // mat Bblock
            pCblock = new int[blocksize * blocksize]; // mat Cblock
            pTempAblock = new int[blocksize * blocksize]; // mat TempBlock
            fillZeros(pCblock);

            DataDistribution(aMatrix, bMatrix, pTempAblock, pBblock, size,
                    blocksize);

            ParallelResultCalculation(pAblock, pTempAblock, pBblock,
                    pCblock, blocksize);

            //TestBlocks(pCblock, blocksize, "Results");

            ResultCollection(cMatrix, pCblock, size, blocksize);

            if (ProcRank == 0) {
                timeStampPar += System.nanoTime() - startTime;
            }
            if (ProcRank == 0){
                //TestResult(aMatrix, bMatrix, cMatrix, size);
                startTime = System.nanoTime();
                SerialResultCalculation(aMatrix, bMatrix, cMatrix, size);
                timeStampSeq += System.nanoTime() - startTime;

            }
        }
            if (ProcRank == 0) {
                out.println(ProcNum + " - Послідовний - " + size + "\n" + timeStampSeq / loopsCount * 0.000001);
                out.println(ProcNum + " - Алгоритм Фокса - " + size + "\n" + timeStampPar /loopsCount * 0.000001);
                timeStampSeq = timeStampPar = 0;
            }
            GridComm.Barrier();
        }

        MPI.Finalize();
    }
}