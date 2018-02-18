/***********************************************************************************
 * @class           Shell                                                          *
 * @description     This Shell facilitates the interactions between the user and   *
 *                  the operating system (ThreadOS). It will allow for users to    *
 *                  processes/programs concurrently and in sequence.               *
 * @author          Cameron Padua                                                  *
 * @date            April 9, 2017                                                  *
 *                                                                                 *
 * Limitations: The program does not handle errors, if bad input is given, it will *
 * throw Java exceptions that files are not found or bad parameters were given     *
 * Assumptions: The user will boot the program using 'java Boot' followed by       *
 * 'l Shell'. Every command will be seperated by a delimiter (& for concurrent and *
 * ; for sequential) with a space between a command and the delimiter. In addition,*
 * if a command does not contain a delimter (only one command or the last of a     *
 * series of commands) the Shell uses a sequential method to running that command. *
 ***********************************************************************************/
import java.io.*;
import java.util.*;

class Shell extends Thread {
    private int commandNumber;  //keep track of current shell command #
    private int prevPID;        //the pervious running process' ID
    private String command;     //a single command parsed from the commands entered
/*------------------------------Constructor-------------------------------------
    The constructor has parameters to be used for intializing the program. However
     the constructr will intialize the command number to start at zero, which
     actually start 1 during the program, the previous PID to -1, and an empty
     single command
 */
    public Shell() {
        commandNumber = 0;
        prevPID = -1;
        command = "";
    }
/*----------------------------------run------------------------------------------
    This method is an inherit method from the Thread class that will run after the
     intialization of the instance of this Shell. This method is responsible for
     printing the "shell[n]%" prompt after the Shell has been started. It is also
     responsible for read the input commands from the user into a StringBuffer
     that will eventually converted into a String[] array and passed to the
     processCommands method. However, before the buffer is converted to a array,
     the method will check if the input was nothing or the key word "exit" to end
     the program. All of this will be repeated in a loop until that keyword has
     been entered to the buffer.
 */
    public void run() {
        StringBuffer s = new StringBuffer();
        while (true) {  //until they enter exit command
            SysLib.cout("Shell[" + ++commandNumber + "]% ");

            SysLib.cin(s);      //read input

            while(s.toString().trim().equalsIgnoreCase("")) { //if no input was
                                                                // entered
                SysLib.cout("Shell[" + commandNumber + "]% ");//reprint command #
                SysLib.cin(s);                                //ask for input
            }

            if(s.toString().equalsIgnoreCase("exit")) //exit case command
                break;

            String[] args = SysLib.stringToArgs(s.toString());
            processCommands(args);      //spilts and runs the commands
            s.setLength(0);
        }
        SysLib.exit();
    }
/*-----------------------------------processCommands-------------------------------
    This method will take a String array of commands that will be parsed into
    individual commands that will be run using their respected run type. This is
    decided by the delimiter that follows each of the commands. The '&' delimiter
    will run the process in the background while another process is being started.
    The ';' delimiter will run the process and wait for termination before moving
    onto the next command/process to be run. If the command is the first and only
    command OR the last of a series of commands, it does not need a delimiter,
    however it will be run as a ';' delimiter or sequential. Every command needs a
    delimiter, unless it is as specified in the previous sentence. This method
    also keeps a running total of the parts added to commands to know if the last
    part of a command has been added to know if it has to default to the ';'
    delimiter.
 */
    private void processCommands(String[] args) {
        int current = 0;            //a running total of the parts added to a
                                    // command
        for (String part : args) {
            command += part;
            if ((part.equals(";")) || (part.equals("&"))) { //if it is a delimiter
                if (part.equals(";")) {     //sequential
                    executeSequentially();
                } else {                    //concurrent
                    executeConcurrently();
                }
            } else {                        //not a delimiter
                if ((current == args.length - 1)) { //contains no delimter in the
                    // command (for single commands and the last of a series of
                    // commands)
                    executeSequentially();//runs as a sequential command
                } else {    //not a delmiter and not the end of the commands
                    command += " "; //add the part to command
                }
            }
            current++;
        }
    }
/*-----------------------------executeSequentially--------------------------------
    This method is very simple and has one purpose, to create and wait for a
    process to finish before returning to the processCommand method. It will take
    the global string command and spilt it into a string array. It will then
    execute the command using the SysLib method exec. That exec method will
    return the PID of the process to this method in which this method will wait
    for the termination of this process. It will then clear the command and return
    to the previous method
 */
    private void executeSequentially() {
        String[] commandArgs = SysLib.stringToArgs(command);
        prevPID = SysLib.exec(commandArgs); //run command
        while (SysLib.join() != prevPID) ;  //wait for the termination of the
                                            // command
        command = "";                       //clear the command
    }
/*-----------------------------executeSequentially--------------------------------
    This method, unlike the previous, spilts the global strig command into a string
    array, runs the commands using the SysLib.exec, clears the command, and
    returns to the  processCommands method without waiting for the termination of
    the process created
*/
    private void executeConcurrently() {
        String[] commandArgs = SysLib.stringToArgs(command);
        prevPID = SysLib.exec(commandArgs); //run command
        command = "";                       //clear command
    }
}
