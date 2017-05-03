package lgp.examples

import lgp.core.environment.*
import lgp.core.environment.config.JsonConfigLoader
import lgp.core.environment.constants.GenericConstantLoader
import lgp.core.environment.dataset.CsvDatasetLoader
import lgp.core.environment.operations.DefaultOperationLoader
import lgp.core.evolution.Runners
import lgp.core.evolution.fitness.FitnessFunctions
import lgp.core.evolution.population.*
import lgp.lib.BaseInstructionGenerator
import lgp.lib.BaseProgramGenerator
import java.util.*

/**
 * An example of setting up an environment to use LGP to find programs for the function `x^2 + 2x + 2`.
 *
 * This example serves as a good way to learn how to use the system and to ensure that everything
 * is working correctly, as some percentage of the time, perfect individuals should be found.
 */
class SimpleFunction {
    companion object Main {
        // Locations of configuration and data set files.
        private val configFilename = this::class.java.classLoader.getResource("simple_function_env.json").file
        private val datasetFilename = this::class.java.classLoader.getResource("simple_function.csv").file

        @JvmStatic fun main(args: Array<String>) {
            // Load up configuration information from the JSON file
            val configLoader = JsonConfigLoader(
                    filename = configFilename
            )

            val config = configLoader.load()

            // Set up a loader to load the data set in the CSV file we specified above
            // Note that the type parameter of the data is automatically inferred from
            // the function given for parsing.
            val datasetLoader = CsvDatasetLoader(
                    filename = datasetFilename,
                    parseFunction = String::toDouble
            )

            // Set up a loader for loading the operations we want to use (specified in the configuration file)
            val operationLoader = DefaultOperationLoader<Double>(
                    operationNames = config.operations
            )

            // Set up a loader for the constant values (specified in the configuration file)
            val constantLoader = GenericConstantLoader<Double>(
                    constants = config.constants,
                    parseFunction = String::toDouble
            )

            // Fill calculation registers with the value 1.0
            val defaultValueProvider = DefaultValueProviders.constantValueProvider<Double>(1.0)

            // Use mean-squared error function for fitness evaluations.
            val mse = FitnessFunctions.MSE()

            // Create a new environment with these components.
            val environment = Environment<Double>(
                    configLoader,
                    constantLoader,
                    datasetLoader,
                    operationLoader,
                    defaultValueProvider,
                    fitnessFunction = mse
            )

            // Set up registered modules
            val container = ModuleContainer(
                    modules = mutableMapOf(
                            CoreModuleType.InstructionGenerator to {
                                BaseInstructionGenerator(environment)
                            },
                            CoreModuleType.ProgramGenerator to {
                                BaseProgramGenerator(environment, sentinelTrueValue = 1.0)
                            },
                            CoreModuleType.SelectionOperator to {
                                TournamentSelection(environment, tournamentSize = 2)
                            },
                            CoreModuleType.RecombinationOperator to {
                                LinearCrossover(
                                        environment,
                                        maximumSegmentLength = 6,
                                        maximumCrossoverDistance = 5,
                                        maximumSegmentLengthDifference = 3
                                )
                            },
                            CoreModuleType.MacroMutationOperator to {
                                MacroMutationOperator(
                                        environment,
                                        insertionRate = 0.67,
                                        deletionRate = 0.33
                                )
                            },
                            CoreModuleType.MicroMutationOperator to {
                                MicroMutationOperator(
                                        environment,
                                        registerMutationRate = 0.5,
                                        operatorMutationRate = 0.3,
                                        constantMutationFunc = { v ->
                                            // Add random gaussian noise to constant with standard deviation of 1
                                            // from the current value.
                                            v + (Random().nextGaussian() * 1)
                                        }
                                )
                            }
                    )
            )

            // Alternatively...
            /*
                environment.registerModule(
                    CoreModuleType.InstructionGenerator,
                    { BaseInstructionGenerator(environment) }
                )
                environment.registerModule(
                    CoreModuleType.ProgramGenerator,
                    { BaseProgramGenerator(environment, sentinelTrueValue = 1.0) }
                )
                ... For each module
             */

            environment.registerModules(container)

            // Find the best individual with these parameters.
            val model = Models.SteadyState(environment)
            //val result = model.evolve()

            //result.best.effectiveInstructions.forEach(::println)
            val runner = Runners.DistributedRunner(environment, model, runs = 10)
            val result = runner.run()

            result.results.forEachIndexed { run, res ->
                println("Run ${run + 1} (best fitness = ${res.best.fitness})")
                res.best.effectiveInstructions.forEach(::println)
                println("\nStats (last run only):\n")

                for ((k, v) in res.statistics.last().data) {
                    println("$k = $v")
                }
                println("")
            }
        }
    }
}

