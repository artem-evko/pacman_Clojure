% prolog/ghost_ai.pl
% Вход (читаем из stdin):
% state(ghost(Xg,Yg), pacman(Xp,Yp), moves([up,down,left,right])).
% Выход (stdout):
% move(left).

:- initialization(main).

main :-
    read(State),
    decide(State, Move),
    write(Move), nl,
    halt.

decide(state(ghost(Xg,Yg), pacman(Xp,Yp), moves(Moves)), move(Best)) :-
    best_move(Moves, Xg, Yg, Xp, Yp, Best).

best_move([M|Ms], Xg, Yg, Xp, Yp, Best) :-
    score_move(M, Xg, Yg, Xp, Yp, S),
    best_move_acc(Ms, Xg, Yg, Xp, Yp, M, S, Best).

best_move_acc([], _, _, _, _, Best, _, Best).
best_move_acc([M|Ms], Xg, Yg, Xp, Yp, CurBest, CurScore, Best) :-
    score_move(M, Xg, Yg, Xp, Yp, S),
    ( S < CurScore ->
        best_move_acc(Ms, Xg, Yg, Xp, Yp, M, S, Best)
    ;   best_move_acc(Ms, Xg, Yg, Xp, Yp, CurBest, CurScore, Best)
    ).

% Чем меньше Манхэттен после шага — тем лучше
score_move(up,    Xg, Yg, Xp, Yp, S) :- Y1 is Yg - 1, manhattan(Xg, Y1, Xp, Yp, S).
score_move(down,  Xg, Yg, Xp, Yp, S) :- Y1 is Yg + 1, manhattan(Xg, Y1, Xp, Yp, S).
score_move(left,  Xg, Yg, Xp, Yp, S) :- X1 is Xg - 1, manhattan(X1, Yg, Xp, Yp, S).
score_move(right, Xg, Yg, Xp, Yp, S) :- X1 is Xg + 1, manhattan(X1, Yg, Xp, Yp, S).

manhattan(X1, Y1, X2, Y2, S) :-
    DX is abs(X1 - X2),
    DY is abs(Y1 - Y2),
    S is DX + DY.
