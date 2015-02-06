package com.metawiring.load.core;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.metawiring.load.generator.GeneratorBindings;

/**
 * A class that allows prepared statements and generatorBindings to be bundled together for easy use
 * within an activity. This is useful for activities that need to juggle multiple types of
 * statements, and probably to simplify others.
 */
public class ReadyStatement {
    private final long startCycle;
    private final PreparedStatement preparedStatement;
    private final GeneratorBindings generatorBindings;

    public ReadyStatement(ExecutionContext context, PreparedStatement preparedStatement, long startCycle) {
        this.preparedStatement = preparedStatement;
        this.generatorBindings = new GeneratorBindings(context.getGeneratorInstanceSource());
        this.startCycle = startCycle;
    }

    public BoundStatement bind() {
        Object[] all = generatorBindings.getAll();
        BoundStatement bound = preparedStatement.bind(all);
        return bound;
    }

    public void addBinding(String varname, String generatorName) {
        generatorBindings.bindGenerator(preparedStatement,varname,generatorName, startCycle);
    }

    public PreparedStatement getPreparedStatement() {
        return preparedStatement;
    }

}
