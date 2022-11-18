##
## ANTI-CAPTCHA AGGREGATE, 2022
## Anti Captcha Aggregate Makefile
## File description:
## Generic Makefile for Anti Captcha Aggregate
##

#=================================
#	Commands
#=================================

.PHONY:				all \
					install \
					test \
					finstall \
					clean

all:				install

install:
					mvn install

finstall:
					mvn install -DskipTests

test:
					mvn test

clean:
					mvn clean
