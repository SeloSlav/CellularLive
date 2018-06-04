# CellularLive
A Twitter bot that continually iterates a cellular automaton in 1D time using Hosebird + Streaming API.

In order to get this running on your own account, create a Twitter applcation at https://apps.twitter.com and set your keys and access tokens in this file: https://github.com/SeloSlav/CellularLive/blob/master/src/main/resources/twitter4j.properties.

Run the program in your favorite IDE then seed it by tweeting ``CAdnauseum x-x-xx--x-x--``

After this, the automaton will iterate every 40 seconds using the last line of output from the previous tweet as the first line of input in the next one. 

Here is a partial example of the bot in action (this is as much as I could fit in a screenshot):

![Image of CA Bot](https://github.com/SeloSlav/CellularLive/blob/master/ca-example.PNG)

