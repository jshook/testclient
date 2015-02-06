package com.metawiring.load.core;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;

import java.util.List;

public class ReadyStatements {
    private ReadyStatement[] readyStatements;

    public ReadyStatements(ReadyStatement... readyStatements) {
        this.readyStatements = readyStatements;
    }

    public ReadyStatements(List<ReadyStatement> readyStatements) {
        this(readyStatements.toArray(new ReadyStatement[readyStatements.size()]));
    }

    public ReadyStatement getNext(long moduloMultiple) {
        int selected = (int) (moduloMultiple % readyStatements.length);
        return readyStatements[selected];
    }

    public ReadyStatements setConsistencyLevel(ConsistencyLevel defaultConsistencyLevel) {
        for (ReadyStatement readyStatment: readyStatements) {
            PreparedStatement preparedStatement = readyStatment.getPreparedStatement();
            preparedStatement.setConsistencyLevel(defaultConsistencyLevel);
        }
        return this;
    }

}
