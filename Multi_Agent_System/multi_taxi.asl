// taxi: a robot that collects the customer from the mars surface

at(P) :- pos(P,X,Y) & pos(taxi,X,Y).

// Manager: an agent responsible for making customer contracts - assigning customers among the taxis

all_proposals_received(CNPId,NP) :-  
     .count(propose(CNPId,_)[source(_)], NO) &  
     .count(refuse(CNPId)[source(_)], NR) &    
     NP = NO + NR.  // num(participants) = num(proposals) + num(refusals)

!cnp(1,deliver(cust(ID))).

!register.

+!register <- .df_register(initiator).

+!cnp(Id,Task)
   <- !call(Id,Task,LP);
      !bid(Id,LP);
      !winner(Id,LO,WAg);
      !result(Id,LO,WAg).

+!call(Id,Task,LP)
   <- .print("Waiting taxis for ",Task,"...");
      .wait(2000);
      .df_search("participant",LP);
      .print("Sending CFP to ",LP);
      .send(LP,tell,cfp(Id,Task)).

+!bid(Id,LP) // the deadline of the CNP is now + 4 seconds (or all proposals received)
   <- .wait(all_proposals_received(Id,.length(LP)), 4000, _).

+!winner(Id,LO,WAg) : .findall(offer(O,A),propose(Id,O)[source(A)],LO) & LO \== []
   <- .print("Offers are ",LO);
      .min(LO,offer(WOf,WAg)); // the first offer is the best
      .print("Winner is ",WAg," with ",WOf).
+!winner(_,_,nowinner). // no offer case

+!result(_,[],_).
+!result(CNPId,[offer(_,WAg)|T],WAg) // announce result to the winner
   <- .send(WAg,tell,accept_proposal(CNPId));
      !result(CNPId,T,WAg).
+!result(CNPId,[offer(_,LAg)|T],WAg) // announce to others
   <- .send(LAg,tell,reject_proposal(CNPId));
      !result(CNPId,T,WAg).

!register.
+!register <- .df_register("participant");
              .df_subscribe("initiator").


// If taxi doesn't have customer, it registers as an available worker.
+!not customer(taxi) : true
   <- @c +cfp(CNPId,Task)[source(A)] : provider(A,"initiator") & price(Task,Offer)
         <- +proposal(CNPId,Task,Offer); // remember my proposal
         .send(A,tell,propose(CNPId,Offer)).
      //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      +!price(Task, Offer) <- pos(taxi, X1, Y1);
                              pos(cust(ID), X2, Y2);
                              Offer is (X1 - X2) + (Y1 - Y2).
      //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      @r1 +accept_proposal(CNPId) : proposal(CNPId,Task,Offer)
         <- .print("My proposal '",Offer,"' won CNP ",CNPId, " for ",Task,"!");
            assert(customer(taxi))

      @r2 +reject_proposal(CNPId)
         <- .print("I lost CNP ",CNPId, ".");
            -proposal(CNPId,_,_).

// If taxi has customer, it registers as an unavailable worker.
+!customer(taxi) : true
   <- @c +cfp(CNPId,_Service)[source(A)] : provider(A,"initiator")
         <- .send(A,tell,refuse(CNPId)).

// If taxi has customer and does not desire to carry it to d, it initiates the action to carry the customer to d. (only if it is WAg. do these)
@lg[atomic]
+customer(taxi) : not .desire(carry_to(d))
   <- !carry_to(d).

// Defines the process to carry the customer to robot R.
+!carry_to(R)
   <- ?pos(taxi,X,Y);
      -+pos(last,X,Y);
      !take(cust(ID),R);
      // !at(last);
      !check(slots).

// Handles the process of taking an item S and dropping it at location L.
+!take(S,L) : true
   <- !ensure_pick(S);
      !at(L);
      drop(S).

// Ensures that if taxi has customer, it will attempt to pick it. Otherwise, it will do nothing.
+!ensure_pick(S) : customer(taxi)
   <- pick(cust(ID));
      !ensure_pick(S).  // Picking up customer is not deterministic, so we need a recursive plan to make sure it happens.
+!ensure_pick(_).

// Manages the movement of taxi toward a specific location L.
+!at(L) : at(L).
+!at(L) <- ?pos(L,X,Y);
           move_towards(X,Y);  // Repeatedly moving one step towards the right location.
           !at(L).
