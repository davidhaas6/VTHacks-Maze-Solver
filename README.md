# MazeWays

An Android application used to solve mazes captured with a phone camera, built using OpenCV.

## Screen Shots
#### Take a picture, select the maze, get your solution.
<img src="https://i.imgur.com/DwB5ivp.png" width="200"> <img src="https://i.imgur.com/vhxw7D8.png" width="200"> <img src="https://i.imgur.com/cNgg7hH.png" width="200">

## Reflection

This was originally built during VTHacks 5 over the course 48 hours. I had never made an app before, so I figured that a time-constrained hackathon would be a good push to finally get me working on one. Since then, I have worked on it on and off, polishing both the UI and the underlying computer vision.

Originally I wanted to build an application that could solve mazes that a user took a picture of. This was based off of a project I built in 10th grade, in which I wrote a program to solve computer-generated mazes. I started this project through prototyping the CV in Python, I then rewrote the CV in Java and added an Android interface.

One of the main challenges I ran into was cleaning up the user's image so that it was suitable to be solved as a maze. Originally, I tried to perform a perspective transform to remove the skew and make the maze more rectangular so that it might be easier for the A* search algorithm to parse. After the hackathon, I scrapped this idea and began to run A* over the skewed maze and it worked fine. Other operations I ran to clean up the image include thresholding and blurring.

The only technologies I used to implement this project are OpenCV and the Android API. My reasoning for using OpenCV was primarily because of the time constraints of the hackathon and my prior experience with the library. However, in the future I hope to eliminate the OpenCV dependencies and rewrite the CV methods from scratch using Android's built-in Matrix class.
