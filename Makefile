ifndef JAVA_HOME
    $(error JAVA_HOME not set)
endif

TARGET=javaresourcemonitor.jar

.PHONY: all clean

all:
	$(JAVA_HOME)/bin/javac *.java
	$(JAVA_HOME)/bin/jar cvfm $(TARGET) MANIFEST.MF *.class

clean:
	rm -f $(TARGET)
	rm -f *.class

