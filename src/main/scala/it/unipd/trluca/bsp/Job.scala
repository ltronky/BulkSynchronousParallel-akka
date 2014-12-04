package it.unipd.trluca.bsp

trait Job[S, T <: Agent[S, T]] {

    def startAgents():List[T]

    def shouldRunAgain(phase:Int):Boolean
}
