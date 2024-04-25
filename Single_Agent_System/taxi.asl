// taxi: a robot that collects the customer from the mars surface

at(P) :- pos(P,X,Y) & pos(taxi,X,Y).

!check(slots).

// If taxi doesn't have customer, it checks the next slot and continues to check slots recursively. Otherwise, it does nothing.
+!check(slots) : not customer(taxi)
   <- next(slot);
      !check(slots).
+!check(slots).

// If taxi has customer and does not desire to carry it to d, it initiates the action to carry the customer to d.
@lg[atomic]
+customer(taxi) : not .desire(carry_to(d))
   <- !carry_to(d).

// Defines the process to carry the customer to robot R.
+!carry_to(R)
   <- ?pos(taxi,X,Y);
      -+pos(last,X,Y);
      !take(cust,R);
      // !at(last);
      !check(slots).

// Handles the process of taking an item S and dropping it at location L.
+!take(S,L) : true
   <- !ensure_pick(S);
      !at(L);
      drop(S).

// Ensures that if taxi has customer, it will attempt to pick it. Otherwise, it will do nothing.
+!ensure_pick(S) : customer(taxi)
   <- pick(cust);
      !ensure_pick(S).  // Picking up customer is not deterministic, so we need a recursive plan to make sure it happens.
+!ensure_pick(_).

// Manages the movement of taxi toward a specific location L.
+!at(L) : at(L).
+!at(L) <- ?pos(L,X,Y);
           move_towards(X,Y);  // Repeatedly moving one step towards the right location.
           !at(L).
