/**
 * Copyright (c) 2018, Sylvain Baudoin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.sbaudoin.sonar.plugins.yaml.checks;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.tokens.KeyToken;
import org.yaml.snakeyaml.tokens.ScalarToken;
import org.yaml.snakeyaml.tokens.Token;
import com.github.sbaudoin.yamllint.LintScanner;

import java.io.IOException;

/**
 * Check to be used that the YAML file does not contain forbidden keys
 */
@Rule(key = "ForbiddenKeyCheck")
public class ForbiddenKeyCheck extends YamlCheck {
    private static final Logger LOGGER = Loggers.get(ForbiddenKeyCheck.class);


    @RuleProperty(key = "key-name", description = "Regexp that matches the forbidden name")
    String keyName;


    @Override
    public void validate() {
        if (yamlSourceCode == null) {
            throw new IllegalStateException("Source code not set, cannot validate anything");
        }

        try {
            LintScanner parser = new LintScanner(new StreamReader(yamlSourceCode.getContent()));
            if (!yamlSourceCode.hasCorrectSyntax()) {
                LOGGER.warn("Syntax error found, cannot continue checking keys: " + yamlSourceCode.getSyntaxError().getMessage());
                return;
            }
            while (parser.hasMoreTokens()) {
                Token t1 = parser.getToken();
                if (t1 instanceof KeyToken && parser.hasMoreTokens()) {
                    Token t2 = parser.getToken();
                    if (t2 instanceof ScalarToken && ((ScalarToken)t2).getValue().matches(keyName)) {
                        // Report new error
                        getYamlSourceCode().addViolation(new YamlIssue(
                                getRuleKey(),
                                "Forbidden key found",
                                t2.getStartMark().getLine() + 1,
                                t2.getStartMark().getColumn() + 1));
                    }
                }
            }
        } catch (IOException e) {
            // Should not happen: a first call to getYamlSourceCode().getContent() was done in the constructor of
            // the YamlSourceCode instance of this check, but in case...
            LOGGER.warn("Cannot read source code", e);
        }
    }
}
