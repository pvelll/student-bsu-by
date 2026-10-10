package github.sushkpavel.studentbsuby.util.communication


interface BroadcastCommunication<T>
    : Communication<T>, Broadcast<T>
