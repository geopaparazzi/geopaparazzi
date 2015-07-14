A use case
================

Use of Geopaparazzi in Egypt to create a map to improve the management of small wastewater treatment plants. 
---------------------------------------------------------------------------------------------------------------

Introduction
+++++++++++++++++

Have you ever tried to type in google: *sanitation maps, sanitation coverage, sanitation or inventory tracking*?

It is interesting to note that the agencies managing the wastewater treatment plants in the United States 
and Canada **monitor and continuously update the inventory of small delocalized plants in order to optimize 
their management**, while the numerous development agencies that deal with *sanitation in developing countries*
express and declare the **state of inadequacy** of the system of waste treatment but **do not quantify the state of art**. 

The management of small treatment plants of domestic sewage in decentralized communities is an
issue with different perspectives. In any place on earth it involves many actors, linking **technical to cultural,
environmental, educational and health services**. It is a topic that often touches critical as well as hidden 
issues. Experts in this field are well aware of that!

When you think of a small village, a rural setting with a few houses and lots of animals, or
a peri-urban area that grew quickly and without any planning or again neighboring communities 
linked by a waterway that carries waste from upstream to valley, it is not immediat to think 
that all these places (due to the presence of man and animal) are a source of biomass.
They are a sources of waste and organic material that often brings a high and invisible pathogenic charge.

**Many projects related to developing countries today focus the attention on the proper disposal 
of the waste and on prevention as fundamental for the health of the inhabitants.** Numerous agencies and 
associations dedicated to development cooperation have shown that untreated wastewater spilled 
into the surrounding environment without any attention is the primary vector of diseases and 
therefore often of epidemics in areas with deficits at levels of hygiene and public health
(see the `Water, Sanitation and Hygiene <http://www.unhcr.org/pages/49c3646cef.html>`_ and
`the Joint Monitoring Programme <http://www.wssinfo.org/about-the-jmp/introduction/>`_). 

The target C of the seventh Millennium Development Goal aimed to halve by 2015 the proportion 
of people without sustainable access to safe water and basic health services. The assessment 
tool implemented in recent years has been the count of the percentage of population using the 
health service. 

But:

* But what is the scenario in terms of plants? 
* Where are they? 
* What do they produce? 
* How much? 
* What is the trend? 
* What are the possible risk factors? 
* What are the environmental characteristics of their location? 
* And the social context? 
* The cultural? 
* How many and which animals contribute? 
* What are the types of installations in operation?

It is clear that to get a **complete picture** of this kind takes **time**, lots of **data collection**, 
**laboratory tests**, and then **hypothesis, correlations, ideas** and possible strategies.

It is now well known that from a technical standpoint there are no prepackaged solutions 
exportable in contexts with very different boundary conditions. Some causes may seem trivial, 
but if underestimated they end up affecting the efficiency of the treatment. 

It should be enough to say that there are places in the world where the toilet paper is not 
thrown into the toilet; there are places where after defecation you clean up with your hand; 
others where even plastic, cloth and diapers are thrown into the toilet
and places in which under the sink there is a shredder and everything ends up into the sewer 
system. 

These are information that decide the diameter of the pipes even before the type of treatment 
and planting. And if the diameter of the tubes is wrong the whole system becomes meaningless. 

How many socio-cultural aspects exist in the various regions of the world that we
do not know, that we can't even imagine, that can make you smile or frighten, but that in 
the construction of a plant have the same weight as the distribution of BOD5?

There is no doubt that a tool that allows you to collect data of this kind, to catalog them, 
and most important geotag them to make them visible on a map and return the state of the existing 
as well as facilitates the correlation is crucial in the whole process of planning and 
improvement of health services in developing contexts.

Monitoring activities 
++++++++++++++++++++++++++

During fall of 2010 Geopaparazzi has been used in the monitoring of **small waste water treatment 
plants in decentralized communities** of the Nile Delta, in a thesis project in Environmental 
Engineering within a larger project of technical development cooperation (`ESRISS project 
<http://www.eawag.ch/forschung/sandec/gruppen/sesp/esriss/index_EN>`_). 

The idea was to be able to do an **effective analysis and mapping of the plants** in the area of 
the Nile Delta to **compare different types of treatment and analyze the parameters that contribute 
to the success or failure of a solution**. The result of this campaign suggested the usefulness 
of this application and the need to widen its use to build the existing maps that consider 
all the complexity of the factors involved in the search for solutions to improve the health 
conditions of the contexts under analysis. Only in this way it is possible to build an updated database 
on the basis of which appropriate solutions can be proposed.

The main goal has been to integrate missing plants data.

To give an idea, information about the available data are shown for some of the plants:

.. figure:: usecase/07_plant1.png
   :align: center
   :width: 600px

   The El Moufty plant, the one with most available information.

.. figure:: usecase/08_plant2.png
   :align: center
   :width: 600px

   The OM SEN plant, with no lab information available and some general information missing.

.. figure:: usecase/09_plant3.png
   :align: center
   :width: 600px

   The EL KOLEAAH plant, with no lab information available and some general information missing.



Technical background
++++++++++++++++++++++++++

Based on the available data a dedicated form has been designed to collect in a standardized
way as much data as possible.

Once the form has been loaded into Geopaparazzi, it has been possible to access it from the 
:ref:`add notes <notes>` section.

.. figure:: usecase/01_tags.png
   :align: center
   :height: 300px

The form has been divided into 5 main sections to make it easier to read:

* General data

.. figure:: usecase/02_general.png
   :align: center
   :height: 300px

* Construction info

.. figure:: usecase/03_construction.png
   :align: center
   :height: 300px

* Technical data

.. figure:: usecase/04_technical.png
   :align: center
   :height: 300px

* Lab data: this section has been added even if it is quite impossible to
  gather lab data in the field. But since the form based notes can be opened 
  and modified at any time, the section has been added for further update

.. figure:: usecase/05_lab.png
   :align: center
   :height: 300px

* Media

.. figure:: usecase/06_media.png
   :align: center
   :height: 300px


The complete json tags file
++++++++++++++++++++++++++++++++++

For completeness the tags file that creates the above form is 
attached here:

.. literalinclude:: usecase/tags.json

Acknowledgement
+++++++++++++++++++++++

This usecase is a very small and superficial extract of the Geopaparazzi 
related part from the master thesis of Vania Zillante: *ANALISI DI TECNOLOGIE
PER LA DEPURAZIONE DEI REFLUI DOMESTICI IN COMUNITÀ DECENTRATE DEL DELTA
DEL NILO E PROPOSTA PER IL TRATTAMENTO DELLA FRAZIONE ORGANICA. APPLICAZIONE
DI UN SOFTWARE OPEN SOURCE PER LA GEOREFERENZIAZIONE DEGLI IMPIANTI ESISTENTI.*.

Focus has been put only on the data collection and form creation. It is beyond
the scope of this document to describe or comment any other of the contents 
of Vania's thesis.

Vania's thesis also won the scolarship prize by ABL **Premio Giovanni Lorenzin**.



