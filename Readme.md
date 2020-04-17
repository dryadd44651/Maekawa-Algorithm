<h1>Distributed System based on Maekawa-Algorithm</h1>
<p>
  <ul>
    <li>Java based Document Distributed System</li>
    <li>Implemented Java Socket (TCP/IP) for Maekawa-Algorithm</li>
  </ul>
</p>
<pre>
dc01~dc09 are 9 different machines

start quorum0-quorum6 in dc01-dc07
start server0 in dc08
start client0-client4 in dc09-dc013

each client will enter critical section(server0) 20 times
each times client will send the request to quorums for token

without yield and fail, deadlock may happen.
without any waiting, the latency will be about 0.2
with waiting, the latency will be about 4~6

(before senting request)
reduce the first waiting time, the node will broadcast the message almost at the same time.
deadlock easily happen.

(during critical section)
reduce second waiting time, the waiting for quorum times and latency reduce significantly.

for more detail please check <a href="https://github.com/dryadd44651/Maekawa-Algorithm/blob/master/CS6378_Fall2019_Project2.pdf"> CS6378_Fall2019_Project1_.pdf</a>



</pre>
