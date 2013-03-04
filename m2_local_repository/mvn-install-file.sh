#!/bin/sh

mvn install:install-file -Dfile=source_libraries/owltools-oort-all.jar -DgroupId=org.bbop -DartifactId=OWLTools-Parent -Dversion=0.2.1-SNAPSHOT -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=repository/

mvn install:install-file -Dfile=source_libraries/orthoxml-0.1b.jar -DgroupId=sbc.orthoxml -DartifactId=orthoxml -Dversion=0.1b -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=repository/

exit 0
