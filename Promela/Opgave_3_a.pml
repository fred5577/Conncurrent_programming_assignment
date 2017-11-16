pid car1, car2;

init{
	atomic {
		car1 = run CAR();	
		car2 = run CAR();
	}
}

short mutex = 1;
short goIn = 0;
short goOut = 1;

short count = 0;
short cars = 2;

short car1In = 0;
short car2In = 0;

inline P(sem){
	atomic{sem > 0 -> sem--};
}

inline V(sem){
	atomic{sem++};
}

/* For testint */
active proctype Check(){
	(car1In - car2In <= 1) && 
	(car2In - car1In <= 1) -> assert(true);
}


proctype CAR(byte number) {
	do
	:: 	P(mutex);
		count++;
		if
		:: count == cars -> P(goOut);
							V(goIn);
		:: count != cars ->	skip;
		fi;
		V(mutex);
		P(goIn);
		V(goIn);
		
		
		P(mutex);

		if
		:: number == 1 ->	car1In++;
		:: number != 1 ->	car2In++;
		fi;
		
		count--;
		if
		:: count == 0 ->	V(mutex);
							P(goIn);
							V(goOut);
		:: count != 0 ->	V(mutex);
		fi;
		P(goOut);
		V(goOut);
	od
}