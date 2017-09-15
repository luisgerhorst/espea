#!/usr/bin/gnuplot
set terminal pdfcairo color size 32cm, 8cm
set output ARG2

set offset 1, 1, 0, 0
set yrange [0:]
# set xlabel 'Problem'
set ylabel 'CPU time (s)'

box_width = 0.33

set key reverse left horizontal Left outside
set xtics nomirror center offset first (box_width/2), 0

set border linewidth 1.5
set style line 1 lc rgb 'grey30' ps 7 lt 1 lw 2
set style line 21 lc rgb 'grey30' lt 1 lw 2

set style line 2 lc rgb 'grey60' ps 7 lt 1 lw 2
set style line 22 lc rgb 'grey30' lt 1 lw 2

set style line 3 lc rgb 'grey60' ps 0 lt 1 lw 2
set style line 23 lc rgb 'grey30' lt 1 lw 2

set style line 4 lc rgb 'grey80' ps 0 lt 1 lw 2
set style line 24 lc rgb 'grey30' lt 1 lw 2

set style fill solid 0.5 border rgb 'grey30'

plot \
ARG1 index 0 \
using ($0):($2 / 1e9):(box_width):xtic(1) title columnheader(1) with boxes ls 1, \
for [SET=1:20] '' index SET \
using ($0+SET*box_width):($2 / 1e9):(box_width) title columnheader(1) with boxes ls (1+SET), \
for [SET=0:20] '' index SET \
using ($0+SET*box_width):($2 / 1e9):($3 / 1e9):($4 / 1e9) notitle columnheader(1) with yerrorbars ls (21+SET), \