# Programming project for Agent Technologies in Research 2014

Agententechnologien in der Forschung (Agent Technologies in Research) is an
annual course at Technische Universität Berlin. Part of it is a project where my
team mate and me had to develop a program for assigning resources to
construction sites. How to model this was largely up to us. It should have
something to do with coalition formation or coalitional skill games, though.

Due to organizational turbulences (not our fault, of course), the outcome didn't
quite reach expectations. The upside was that I could write the whole thing in
Clojure. The resulting program is not spectacular – a fairly primitive
implementation of [tabu search](https://en.wikipedia.org/wiki/Tabu_search) – nor
does it give very good results, but we still learned some valuable things:

## Lessons learned

### Clojure rulez.

Clojure (including its ecosystem) worked surprisingly well for quickly getting
results. Sure, implementing the tabu search algorithm was the usual functional
thing, involving some rethink from the old imperative habits. But we did need
some little extras and we found very nice ones:

 - A ring buffer for the tabu list – easily implemented by hand, but not
   reinventing the wheel is better usually. Take [this](https://github.com/amalloy/ring-buffer) one.
 - The algorithm spits out text, which is not very good for presentation. Take
   [Rhizome](https://github.com/ztellman/rhizome) for visualization. I never
   used it, but it works like a breeze.
 - The pictures were good, but we needed to put them side-by-side for a
   before/after comparison. Good old HTML helped and I like
   [Enlive](https://github.com/cgrand/enlive) for templating
   without templates.

Discovering and using these libraries, especially Rhizome, was great fun after
hard hours of debugging.

### Top-down debugging is no good.

In the middle of implementing the algorithm I just kept coding, coding, coding
without trying out what I wrote. I thought I knew what I was doing and where I
was going. Later when I didn't get the results I expected, I had to dig into the
code several times and fix silly little bugs. Gosh, Clojure does have a REPL for
interactive development. Use it and try out the things you build before you put
them together! It takes so much less time trying to find out what you did wrong
five minutes ago rather than a day, when what you wrote then is already buried
deep in the call chain. I had known about all this before, but had never
experienced it as severely as this time.

### You really have to think how to represent your data.

Another thing that should be clear. But in my algorithmics courses I had never
had issues with data representation, so somehow I thought it always magically
gets itself right. To venture a hypothesis: maybe imperative style allows you to
work around bad data representation for a longer time until it hits back really
hard? Anyway, in this project I had started out with some sort of manual
input-friendly data format, which turned out not to be workable at all during
the first experiments implementing tabu search. So I came up with something
different, which probably isn't ideal, but did it's job much better.

### Don't be shy to transform your data.

In systems where users click buttons (there are many of them) it is clear that
the data coming from the users can't be fed into the core algorithms directly.
We have to transform it first and we have to transform it again when it comes
back.

In a fair number of systems developed in Clojure, data coming from users is
written in EDN and therefore easily machine-readable. Don't let this fool you!
Users are still users and machines are still machines. How a user likes to write
data can be very different from how the machine likes to read them. You need to
design the shape of data to be used by users and the shape of data to be used by
the algorithm and transform between them.

## Usage

This project had to be finished very quickly and was never intended for future
use. For this reason, it is rather untidy and contains strange things like a
file with German contents. You can look at the pictures and play around with the
code at the REPL, though. See `dev/user.clj`.

## Copyright and License

See `LICENSE.txt`.
