start quorum0~quorum6 in dc01.utdallas.edu~dc07.utdallas.edu
start server0 in dc08.utdallas.edu
start client0~client4 in dc09.utdallas.edu~dc013.utdallas.edu

each client will enter critical section(server0) 20 times
each times client will send the request to quorums for token

without yield and fail, deadlock may happen.
between release and request must have sleep time.
because release may happen before request, the client would get token(request) and lose token impudently (release).