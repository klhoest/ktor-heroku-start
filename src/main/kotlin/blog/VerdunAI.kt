package blog

import org.nield.kotlinstatistics.Centroid
import java.util.*
import kotlin.collections.ArrayList

class VerdunAI(val localCluster: Centroid<WorkablePlanet>,val AIList: List<VerdunAI>): AItoPlanet {

    var solarSystemCell = localCluster.points.sortedDescending()
    var sortRebelPlanets = solarSystemCell.filterIsInstance<PlanetRebel>()
    var rebelInside: Int = 0
        get() {
            var result = 0
            for (inspectPlanet in solarSystemCell) {
                result += inspectPlanet.rebelionFleetIncoming
                if (inspectPlanet is PlanetRebel)
                    result += inspectPlanet.colony.units!!
            }
            return result
        }
    val overPopulatedPlanetList: ArrayList<PlanetRebel>
        get() {
            val result = ArrayList<PlanetRebel>()
            sortRebelPlanets
                    .filterTo(result) { it.isOverpopulated }
            return result
        }
    var inerTarget: InerTarget? = null;
    var outerTarget: OuterTarget? = null;

    override fun getRebelInCluster(): Int {
        return rebelInside
    }

    fun constructTargeting() {
        inerTarget = InerTarget()
        outerTarget = OuterTarget();
    }

    fun print() {
        solarSystemCell.forEach {
            var extraTab = "\t"
            if (it.colony.owner == WorkablePlanet.ME) {
                extraTab = "\tX "
            }
            println(extraTab + "planet${it.colony.id}, of owner ${it.colony.owner}, interset:${it.interest}, gr:${it.colony.gr}")
        }
    }

    interface Targeting {
        val target: WorkablePlanet?
        val requiredArmy: Int
        var AIFleetOrders: ArrayList<FleetOrder>
        var sentFleetNumber: Int
        fun findTarget(): WorkablePlanet?
        fun mobilise()
        fun itsATrap() {
            AIFleetOrders.clear()
        }
    }

    inner class InerTarget : Targeting {

        override val target: WorkablePlanet? = findTarget()
        override val requiredArmy: Int
            get() {
                var extraCare = 0
                if(target != null) {
                    if (target.colony.owner != WorkablePlanet.Guilde_du_Commerce) {
                        extraCare = 5 + target.colony.gr!! * 2
                    }
                    return extraCare + target.empireFleetIncoming + (target.enemyCivilainNearby - target.rebelCivilianNearby).toInt() - target.rebelionFleetIncoming - sentFleetNumber
                } else {
                    return 0
                }
            }
        override var AIFleetOrders = ArrayList<FleetOrder>();
        override var sentFleetNumber: Int = 0
            get() = AIFleetOrders.sumBy { inpectFleet -> inpectFleet.units }

        override fun findTarget(): WorkablePlanet? {
            var inspectPlanet: WorkablePlanet
            val it = solarSystemCell.iterator()
            while (it.hasNext()) {
                inspectPlanet = it.next()
                if (!inspectPlanet.safe) {
                    System.out.println("innerTarget : " + inspectPlanet.colony.id + " , interest = " + inspectPlanet.interest)
                    return inspectPlanet
                }
            }
            System.out.println("no innerTarget found. skip this turn");
            return null
        }

        override fun mobilise() {
            if(target != null) {
                println("on innerTarget: " + target.colony.id)
                var mobiliseOverpopulation = false
                for (inspectedPlanet in overPopulatedPlanetList) {
                    if (requiredArmy < 0)
                        return

                    sentFleetNumber = inspectedPlanet.pourFrodon(target.colony.id, requiredArmy, AIFleetOrders)
                    mobiliseOverpopulation = true;
                }
                val it = sortRebelPlanets.iterator()
                while (requiredArmy > 0 && it.hasNext()) {
                    sentFleetNumber += it.next().pourFrodon(target.colony.id, requiredArmy, AIFleetOrders)
                }
                if (requiredArmy > 15 && !mobiliseOverpopulation) {
                    itsATrap();
                }
                returnJSON.fleets.addAll(AIFleetOrders)
            }
        }
    }

    inner class OuterTarget() : Targeting {
        override val target: WorkablePlanet? = findTarget()
        override val requiredArmy: Int
            get() {
                var extraCare = 0
                if(target!= null) {
                    if (target.colony.owner != WorkablePlanet.Guilde_du_Commerce) {
                        extraCare = 30 + target.colony.gr!! * 3
                    }
                    return extraCare + target.empireFleetIncoming + (target.enemyCivilainNearby - target.rebelCivilianNearby).toInt() - target.rebelionFleetIncoming - sentFleetNumber + (distance/70).toInt()
                } else {
                    return 0
                }
            }
        override var AIFleetOrders = ArrayList<FleetOrder>();
        override var sentFleetNumber: Int = 0
            get() = AIFleetOrders.sumBy { inpectFleet -> inpectFleet.units }

        var distance:Double =99999.0
            get() {
                if(target!= null) {
                    return Math.hypot(localCluster.center.x - target.colony.x, localCluster.center.y - target.colony.y)
                }
                else {
                    return 99999.0
                }
            }

        override fun findTarget(): WorkablePlanet? {
            val cellTargets = TreeMap<Double, WorkablePlanet>()
            AIList.forEach { inspectAI ->
                val targetCopy = inspectAI.inerTarget; //could not smart cast otherwise
                if (targetCopy != null && targetCopy.target!= null) {
                    val potentialtarget = targetCopy.target
                    val key = (potentialtarget.interest - Math.hypot(localCluster.center.x - potentialtarget.colony.x, localCluster.center.y - potentialtarget.colony.y) / 40) * -1
                    cellTargets.put(key, potentialtarget)
                }
            }
            if (cellTargets.isEmpty()) {
                System.out.println("no outerTarget found. skip this turn");
                return null
            } else {
                return cellTargets.firstEntry().value
            }
        }

        override fun mobilise() {
            if(target!= null) {
                println("on outerTarget: " + target.colony.id)
                var mobiliseOverpopulation = false
                for (inspectedPlanet in overPopulatedPlanetList) {
                    if (requiredArmy < 0)
                        return
                    sentFleetNumber = inspectedPlanet.pourFrodon(target.colony.id, requiredArmy, AIFleetOrders)
                    mobiliseOverpopulation = true;
                }
                val it = sortRebelPlanets.iterator()
                while (requiredArmy > 0 && it.hasNext()) {
                    sentFleetNumber += it.next().pourFrodon(target.colony.id, requiredArmy, AIFleetOrders)
                }
                if (requiredArmy > 0 && !mobiliseOverpopulation) {
                    itsATrap();
                }
                    returnJSON.fleets.addAll(AIFleetOrders)
            }
        }
    }
}