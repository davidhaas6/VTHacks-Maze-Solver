package com.davidhaas.mazesolver.pathfinding;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.Stack;

public class Tester {


    
    public static void print(int[][] array) {
        String output = "";
        for (int i = 0; i<array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                output=output+array[i][j]+" ";
            }
            output=output+"\n";
        }
        System.out.println(output);
    }
/*
    public static void drawOnImage(Stack<int[]> path, String str) {
        File file = new File(str);
        BufferedImage image;
        int[][] array = getImage(str);
        int lineWidth = 15;
        try {
            image = ImageIO.read(file);
            Color red = new Color(255, 0, 0); // Color red
            int rgb = red.getRGB();
            while(!path.isEmpty()) {
                int[] ij = path.pop();//pop off the coordinated
                image.setRGB(ij[1], ij[0], rgb);//prints the coordinates
                for (int i = 1; i < lineWidth; i++) {
                    try {
                        if(array[ij[0]][ij[1]+i]==0)
                        {
                            image.setRGB(ij[1]+i, ij[0], rgb);
                        }
                        else {
                            break;
                        }
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        //this should not do anything
                    }
                }
                    for (int i = -1; i > -lineWidth; i--) {
                        try {
                            if(array[ij[0]][ij[1]+i]==0)
                            {
                                image.setRGB(ij[1]+i, ij[0], rgb);
                            }
                            else {
                                break;
                            }
                        }
                        catch (ArrayIndexOutOfBoundsException e) {
                            //this should not do anything
                        }
                    }
                for (int j = 0; j < lineWidth; j++) {
                    
                    try {
                        if(array[ij[0]+j][ij[1]]==0)
                        {
                            image.setRGB(ij[1], ij[0]+j, rgb);
                        }
                        else {
                            break;
                        }
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        //this should not do anything
                    }
                }
                for (int j = -1; j > -lineWidth; j--) {
                    
                    try {
                        if(array[ij[0]+j][ij[1]]==0)
                        {
                            image.setRGB(ij[1], ij[0]+j, rgb);
                        }
                        else {
                            break;
                        }
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        //this should not do anything
                    }
                }
                
            }
            File outputfile = new File("savedNew.png");
            ImageIO.write(image, "png", outputfile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            image=null;
        }*/

}
