# Simplification round baseline

Counts include nested packages; world is excluded. Null checks count explicit equality comparisons. ForTest counts identifier calls/declarations (audited separately before removal). Small files have fewer than 40 nonblank, noncomment code lines excluding package/import and standalone braces. FQNs include java.util/io/lang/nio/net/time/awt outside imports. These lexical metrics indicate review candidates, not design quality.

| Scope/package | Files | Lines | Null checks | Wildcards | Console | JDK FQNs | ForTest references | Small files |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| main/(root) | 1 | 12 | 0 | 0 | 0 | 0 | 0 | 1 |
| main/app | 125 | 9130 | 94 | 17 | 4 | 7 | 0 | 84 |
| main/audio | 46 | 5243 | 42 | 4 | 5 | 0 | 0 | 23 |
| main/config | 2 | 393 | 5 | 0 | 6 | 0 | 0 | 0 |
| main/cutscene | 20 | 2552 | 33 | 9 | 0 | 0 | 0 | 9 |
| main/dialog | 6 | 896 | 10 | 0 | 0 | 0 | 0 | 2 |
| main/entity | 80 | 10596 | 147 | 83 | 0 | 0 | 0 | 33 |
| main/equipment | 19 | 1821 | 18 | 2 | 0 | 0 | 3 | 10 |
| main/gameplay | 20 | 1052 | 22 | 0 | 0 | 0 | 0 | 14 |
| main/gpu | 11 | 1183 | 10 | 0 | 0 | 1 | 0 | 2 |
| main/input | 3 | 246 | 1 | 2 | 2 | 1 | 0 | 2 |
| main/physics | 2 | 445 | 10 | 0 | 0 | 0 | 0 | 0 |
| main/recording | 3 | 333 | 5 | 0 | 1 | 2 | 0 | 1 |
| main/render | 48 | 5140 | 216 | 0 | 0 | 0 | 0 | 22 |
| main/rom | 15 | 1363 | 0 | 0 | 0 | 0 | 0 | 8 |
| main/save | 7 | 1253 | 3 | 3 | 1 | 0 | 0 | 2 |
| main/scene | 4 | 186 | 0 | 0 | 0 | 0 | 0 | 3 |
| main/startup | 4 | 447 | 0 | 0 | 0 | 0 | 0 | 1 |
| main/state | 24 | 1578 | 1 | 22 | 0 | 0 | 0 | 14 |
| main/ui | 21 | 2925 | 32 | 0 | 0 | 0 | 0 | 6 |
| main/vfx | 4 | 531 | 7 | 1 | 0 | 0 | 0 | 1 |
| test/(root) | 4 | 824 | 1 | 14 | 2 | 1 | 0 | 0 |
| test/app | 42 | 3414 | 0 | 69 | 12 | 15 | 0 | 18 |
| test/architecture | 5 | 579 | 2 | 3 | 0 | 5 | 0 | 1 |
| test/audio | 42 | 4146 | 9 | 30 | 8 | 10 | 0 | 15 |
| test/config | 3 | 272 | 0 | 2 | 0 | 0 | 0 | 1 |
| test/cutscene | 8 | 827 | 3 | 1 | 0 | 2 | 0 | 3 |
| test/dialog | 6 | 735 | 1 | 3 | 2 | 0 | 0 | 3 |
| test/entity | 36 | 6791 | 5 | 16 | 1 | 5 | 0 | 10 |
| test/equipment | 14 | 2048 | 3 | 2 | 0 | 1 | 6 | 5 |
| test/gameplay | 8 | 1487 | 6 | 1 | 0 | 2 | 0 | 2 |
| test/gpu | 13 | 703 | 4 | 1 | 1 | 0 | 0 | 11 |
| test/input | 2 | 58 | 0 | 2 | 0 | 0 | 0 | 2 |
| test/physics | 1 | 280 | 0 | 0 | 0 | 0 | 0 | 0 |
| test/recording | 2 | 214 | 0 | 2 | 0 | 1 | 0 | 1 |
| test/render | 51 | 5154 | 25 | 36 | 4 | 7 | 0 | 20 |
| test/rom | 11 | 611 | 0 | 9 | 0 | 0 | 0 | 7 |
| test/save | 7 | 994 | 0 | 6 | 0 | 4 | 0 | 2 |
| test/scene | 2 | 152 | 1 | 1 | 0 | 0 | 0 | 1 |
| test/startup | 5 | 337 | 0 | 1 | 1 | 0 | 0 | 2 |
| test/state | 12 | 1053 | 0 | 9 | 0 | 1 | 0 | 9 |
| test/testing | 5 | 108 | 3 | 2 | 0 | 2 | 0 | 5 |
| test/ui | 18 | 1622 | 0 | 11 | 1 | 3 | 0 | 7 |
| test/vfx | 5 | 425 | 4 | 1 | 0 | 0 | 0 | 3 |
