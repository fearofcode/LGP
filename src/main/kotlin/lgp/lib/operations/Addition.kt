package lgp.lib.operations

import lgp.core.evolution.instructions.BinaryOperation
import lgp.core.evolution.registers.Arguments
import lgp.core.modules.ModuleInformation

/**
 * Performs addition on two Double arguments.
 *
 * @suppress
 */
class Addition : BinaryOperation<Double>(Addition.Companion::add) {

    companion object {
        fun add(args: Arguments<Double>): Double {
            return args.get(0) + args.get(1)
        }
    }

    override val representation: String
        get() = " + "

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "An operation for performing the addition function on two Double arguments."
    }

}