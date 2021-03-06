#define UP 0
#define DOWN 1

pid up1, up2, down1, down2;

init{
	atomic {
		up1 = run CAR(UP);
		up2 = run CAR(UP);
		down1 = run CAR(DOWN);
		down2 = run CAR(DOWN);
	}
}

short up = 1;
short down = 1;
short read = 1;
short counter = 0;
short counterD = 0;
short counterU = 0;

/* For testing */
short countUpwardInAlley = 0;
short countDownwardInAlley = 0;

active proctype Check()
{
	!((countUpwardInAlley==0 && countDownwardInAlley==0) ||
	(countUpwardInAlley!=0 && countDownwardInAlley==0) ||
	(countUpwardInAlley==0 && countDownwardInAlley!=0)) -> assert(false)
}

inline P(sem){
	atomic{sem > 0 -> sem--};
}

inline V(sem){
	atomic{sem++};
}

inline enter(dir){
	if
	:: dir == DOWN ->		P(down);
							counterD++;
							if
							:: counterD == 1 -> P(read);
							:: counterD != 1 -> skip
							fi;
							V(down);
	:: dir == UP -> 		P(up);
							counterU++;
							if
							:: counterU == 1 -> P(read);
							:: counterU != 1 -> skip
							fi;
							V(up);

	fi;
}

inline leave(dir){
	if
	:: dir == DOWN -> 		P(down);
							counterD--;
							if
							:: counterD == 0 -> V(read);
							:: counterD != 0 -> skip
							fi;
							V(down);
	:: dir == UP ->			P(up);
							counterU;
							if
							:: counterU == 0 -> V(read);
							:: counterU != 0 -> skip
							fi;
							V(up);
	fi;
}

proctype CAR(byte dir) {
	do
	::
		enter(dir);
		/* CRITICAL, USed for evaluation purposes */
		if
		:: dir == DOWN ->
							countDownwardInAlley++;
							countDownwardInAlley--;
		:: dir == UP ->
							countUpwardInAlley++;
							countUpwardInAlley--;
		fi;
		/* CRITICAL */
		leave(dir);
	od
}