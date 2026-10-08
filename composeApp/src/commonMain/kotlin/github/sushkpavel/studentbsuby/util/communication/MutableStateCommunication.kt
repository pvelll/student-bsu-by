package github.sushkpavel.studentbsuby.util.communication

interface MutableStateCommunication<T> : StateCommunication<T>, StateMapper<T>,
    MutableCommunication<T>
