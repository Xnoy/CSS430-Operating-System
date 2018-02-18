/*******************************************************************************
 * @class           Processes                                                  *
 * @description     This standalone program mimics the linux command "ps -A |  *
 *                  grep argv[1] | wc -l", by using forks, pipes, and a        *
 *                  variety of if statements in assorted methods, to find the  *
 *                  number of occurences of a given process in the running     *
 *                  processes                                                  *
 *                                                                             *
 * @author          Cameron Padua                                              *
 * @date            April 9, 2017                                              *
 *                                                                             *
 * Limitations:                                                                *
 *                                                                             *
 * Assumptions: The user will compile the program using the command 'g++       *
 * processes.cpp -o processes' followed by the command './processes argv[1]'   *
 * with argv[1] being the process that you want to find the count of.          *
 ******************************************************************************/
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <iostream>

using namespace std;
enum {
    READ, WRITE
};
pid_t pid, pidGC, pidGGC;
int pipeFD[2];
int pipeFD2[2];
char *cmd;      //process to find in the grep process
/* ---------------------handlePS()--------------------------------
 * Description: This method is the last method to be called in the execution
 * of the program. It is responsible for closing the entire first pipe and a
 * portion of the second pipe to allow for input to be placed inside. Then
 * it will execute a overriding process that will get a list of  all the
 * running process and place that list into the pipe for the parent process
 * to read from
 */
void handlePS() {           //getting all the process currently running
    close(pipeFD2[READ]);   //close the unused first pipe and the read of the
                            // second pipe
    close(pipeFD[READ]);
    close(pipeFD[WRITE]);
    dup2(pipeFD2[WRITE], 1); //change write to the second pipe
    execlp("ps", "ps", "-A", NULL); //execute the PS process over this
}
/*--------------------------handleGrep()---------------------------------
 * This program is responsible for managing the second process called grep.
 * It has to create the second pipe that will be used between this process
 * and the process that it will fork to. It will then close the read section
 * of the first pipe and close the write section of the second pipe. Then
 * it will change the current process' read and write locations to the
 * second and first pipes respectively. Next it will wait for the child
 *process to run and terminate before moving on. Finally it will overwrite the
 * current process with the grep process looking for the arg that the user
 * passed in. It will then place it's output in the first pipe for it's
 * parent to read.
 */
void handleGrep() {
    if ((pipe(pipeFD2) < 0)) {
        perror("ERROR in making the second pipe");
        exit(EXIT_FAILURE);
    }
    if ((pidGGC = fork()) < 0) {
        perror("Fork Error Great Grand Child");
        exit(EXIT_FAILURE);
    } else if (pidGGC == 0) {   //run the great grand child process
        handlePS();
    } else {                    //run grand child process and overlay with grep
        wait(NULL);
        close(pipeFD2[WRITE]);  //close the unused write of the second pipe
        close(pipeFD[READ]);    //and the read of the first pipe
        dup2(pipeFD[WRITE], 1); //change the write from terminal to pipe 1
        dup2(pipeFD2[READ], 0); //and the read to pipe 2
        execlp("grep", "grep", cmd, NULL);  //execute the grep process over
    }
}
/*----------------------------handleWC()----------------------------
 * This method is responsible for creating the first pipe that will allow
 * for communication between the parent and child processes. It then will
 * either wait for the child termination or run the the handleGrep() method.
 * Then it will close hte write of the first pipe and change the read
 * location to the same pipe. note that the second pipe does not exist in
 * this context. Finally it will overwrite the current process with the wc
 * process to count he number of line and printing it to the console.
 */
void handleWC() {
    if ((pipe(pipeFD) < 0)) {
        perror("ERROR in making the first pipe");
        exit(EXIT_FAILURE);
    }
    if ((pidGC = fork()) < 0) {
        perror("Fork Error Grand Child");
        exit(EXIT_FAILURE);
    } else if (pidGC == 0) {  //run the grand child processes
        handleGrep();
    } else {                //run the child process and overlay with wc
        wait(NULL);
        close(pipeFD[WRITE]);//close the unused write of pipe one
        dup2(pipeFD[READ], 0);//change the read to pipe one
                                //note that pipe 2 does not exist in this
                                // context
        execlp("wc", "wc", "-l", NULL);//execute the ec process over

    }
}
/*------------------------main----------------------
 * The main is responsible for creating the first child process and forking.
 * It will then call the child method that will be the first in the chain of
 * method calls and process creations. The parent process will wait for all
 * child processes to be terminated before moving terminating itself.
 */
int main(int argc, char **argv) {
    if(argc < 1){
        perror("Not Enough arguments");
        exit(EXIT_FAILURE);
    }
    pid = fork();
    if (pid < 0) {
        perror("Fork Error Child");
        exit(EXIT_FAILURE);
    } else if (pid == 0) {       //run the child process
        cmd = argv[1];           //store the grep argument for later
        handleWC();
    } else {                     //wait for child termination
        wait(NULL);
    }
}