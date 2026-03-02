# 0.4

## 0.4.2
- Improved defabrication swarm autofire AI to stop ships from sending like a trillion fragments after a single molecule of a dead hull
- Added graphics for the planned defabrication unit and the in progress locust cannon made by kirbo.1 in the USC
  - Thanks, by the way!

## 0.4.1
- Added VersionChecker support!
- Set range of defab swarms to like a billion as a stopgap for them despawning past max range & overly aggressive fabricators
  - let me know if this causes issues (beyond visual funkiness)
- Fixed bug with defabs and fragment transfer that let you go beyond 100% CR
  - (i assumed that getCurrentCR max was 100 instead of 1)
- Made transfers consider the amount of fragments a ship already has so that like a billion fragments don't build up on one ship
- Disabled Reclamation Swarms, let's see how this goes
- Fixed bug with fragment production caps not applying correctly

***

# 0.3.0

## 0.3.6
- Fixed divide by zero in autofire code
  - Please let this be the last one

## 0.3.5
- Fixed casting error introduced in 0.3.3
  - i also changed how the code for defab swarm spam worked so it should run faster too
- Made fighters no longer count as viable targets for defabs

## 0.3.4
- Fixed null pointer error in 0.3.1
  - thanks to sparranova for finding this

## 0.3.3
- Nerfed fragment production speed to only go up to 2x the maximum that cargo capacity gives you
  - (frigates shouldn't be pumping out as much as a supercap after a good meal, should they?)
- Fixed defabrication AI to not spam as many swarms (hopefully)
  - (reduces chance of firing at any given ship based on the amount of swarms already alive and targeting them)
- Fixed defabrication AI not going around shields
  - (good lord that one sucked, i don't even wanna say how many times i messed up)

## 0.3.2
- Made `getCargoCapacity` account for stat modifications
  - Thanks to ruddygreat for helping me in #advanced-modmaking in the discord this time and multiple others before
- Added GPL-3 license

## 0.3.1
- started keeping track of my changes
- implemented fragment production decay
  - this and the CRPoints variable are stored as a long because i was lazy but i'll probably implement overflow handling to save on memory later
  - you really shouldn't be getting to the upper bounds of an integer anyways
- initial fragment production & fragment capacity increases logarithmically with `1.0076` as a base.
  - this pegs the fabricator with 2000 cargo capacity at 10 fragments per second and 1000 max fragments
  - fragment production decays with each fragment produced
  - can be recovered with defabrication swarms
- removed the CRPoints debug testing thing from the fragment swarm hullmod display and replaced it with the fragment production per second

