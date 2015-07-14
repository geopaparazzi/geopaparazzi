.. index:: forms
.. _forms:

Using form based notes
=======================

Geopaparazzi supports complex notes called *form based notes*.
To use them, a **tags.json** file needs to be created and placed inside 
the geopaparazzi folder inside the sdcard. In that file 
a `json format <http://en.wikipedia.org/wiki/JSON>`_
description of the wanted tags and forms needs to be placed. 

By default, to help the user to start, a sample tags.json is created 
in the Geopaparazzi installation. It contains form samples and examples
to show all supported form widgets.

.. figure:: 08_forms/01_tags.png
   :align: center
   :height: 300px

Currently no tool is available to create forms in an interactive way, 
they have to be created manually. 

A good online tool to at least validate your json form is the `Json Lint 
Validator <http://www.jsonlint.com/>`_. Make sure it passes that test before 
putting it on the device and test it in geopaparazzi.

.. index:: form tags
.. _tags:

Supported tags (via the example form)
-------------------------------------------

The example form that is available by default shows all the possible options available.

.. _textform:

Text
+++++++++

+------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                         |   Created by                                                                        |
+====================================+=====================================================================================+
|                                    |                                                                                     |
| .. figure:: 08_forms/02_text.png   |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                   |     :language: json                                                                 |
|   :height: 300px                   |     :lines: 96-103                                                                  |
|                                    |     :dedent: 10                                                                     |
+------------------------------------+-------------------------------------------------------------------------------------+

Text with key and label
++++++++++++++++++++++++++

In simple forms the key element is also used as label for the widget (ex. *some text* in the :ref:`text note <textform>`).
Some charactersets are not suitable to be used as keys for a database, so in that cases the user should consider
to define both the key (ex. in English) and the label (in the own language). Here an example:

+----------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                             |   Created by                                                                        |
+========================================+=====================================================================================+
|                                        |                                                                                     |
| .. figure:: 08_forms/03_text_key.png   |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                       |     :language: json                                                                 |
|   :height: 300px                       |     :lines: 105-113                                                                 |
|                                        |     :dedent: 10                                                                     |
+----------------------------------------+-------------------------------------------------------------------------------------+

Numbers
+++++++++++++++++

+------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                         |   Created by                                                                        |
+====================================+=====================================================================================+
|                                    |                                                                                     |
| .. figure:: 08_forms/04_numeric.png|   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                   |     :language: json                                                                 |
|   :height: 300px                   |     :lines: 115-126                                                                 |
|                                    |     :dedent: 10                                                                     |
+------------------------------------+-------------------------------------------------------------------------------------+

Labels rendered in the map view
++++++++++++++++++++++++++++++++++

Text and numbers can also be shown as labels in the map view.

To do so, the tag **islabel** has to be added and set to true:

.. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json
  :emphasize-lines: 6
  :language: json                                                                
  :lines: 128-140                                                                

Only one text or number item is accepted as label, so if more than one is added, the last read will be picked.

Date
++++++

+------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                         |   Created by                                                                        |
+====================================+=====================================================================================+
|                                    |                                                                                     |
| .. figure:: 08_forms/05_date.png   |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                   |     :language: json                                                                 |
|   :height: 300px                   |     :lines: 142-149                                                                 |
|                                    |     :dedent: 10                                                                     |
+------------------------------------+-------------------------------------------------------------------------------------+

Time
++++++

+------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                         |   Created by                                                                        |
+====================================+=====================================================================================+
|                                    |                                                                                     |
| .. figure:: 08_forms/06_time.png   |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                   |     :language: json                                                                 |
|   :height: 300px                   |     :lines: 151-158                                                                 |
|                                    |     :dedent: 10                                                                     |
+------------------------------------+-------------------------------------------------------------------------------------+

Labels
++++++++++++

+------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                         |   Created by                                                                        |
+====================================+=====================================================================================+
|                                    |                                                                                     |
| .. figure:: 08_forms/07_labels.png |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                   |     :language: json                                                                 |
|   :height: 300px                   |     :lines: 160-176                                                                 |
|                                    |     :dedent: 10                                                                     |
+------------------------------------+-------------------------------------------------------------------------------------+

Checkbox
+++++++++++++

+------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                         |   Created by                                                                        |
+====================================+=====================================================================================+
|                                    |                                                                                     |
| .. figure:: 08_forms/08_boolean.png|   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                   |     :language: json                                                                 |
|   :height: 300px                   |     :lines: 178-185                                                                 |
|                                    |     :dedent: 10                                                                     |
+------------------------------------+-------------------------------------------------------------------------------------+

Combos
++++++++++

+------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                         |   Created by                                                                        |
+====================================+=====================================================================================+
|                                    |                                                                                     |
| .. figure:: 08_forms/09_combos.png |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                   |     :language: json                                                                 |
|   :height: 300px                   |     :lines: 187-262                                                                 |
|                                    |     :dedent: 10                                                                     |
+------------------------------------+-------------------------------------------------------------------------------------+

Pictures
+++++++++++++

+---------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                            |   Created by                                                                        |
+=======================================+=====================================================================================+
|                                       |                                                                                     |
| .. figure:: 08_forms/10_pictures.png  |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                      |     :language: json                                                                 |
|   :height: 300px                      |     :lines: 264-271                                                                 |
|                                       |     :dedent: 10                                                                     |
+---------------------------------------+-------------------------------------------------------------------------------------+


Sketches
+++++++++++++

+---------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                            |   Created by                                                                        |
+=======================================+=====================================================================================+
|                                       |                                                                                     |
| .. figure:: 08_forms/11_sketches.png  |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                      |     :language: json                                                                 |
|   :height: 300px                      |     :lines: 273-280                                                                 |
|                                       |     :dedent: 10                                                                     |
+---------------------------------------+-------------------------------------------------------------------------------------+


Map screenshot
++++++++++++++++++++

+---------------------------------------+-------------------------------------------------------------------------------------+
| Looks like                            |   Created by                                                                        |
+=======================================+=====================================================================================+
|                                       |                                                                                     |
| .. figure:: 08_forms/12_map.png       |   .. literalinclude:: ../../geopaparazzi-git/geopaparazzi.app/assets/tags/tags.json |
|   :align: center                      |     :language: json                                                                 |
|   :height: 300px                      |     :lines: 282-289                                                                 |
|                                       |     :dedent: 10                                                                     |
+---------------------------------------+-------------------------------------------------------------------------------------+


Other supported tags
++++++++++++++++++++++++++

hidden 
~~~~~~~

Not shown in the gui, but useful for the application to fill in infos like the GPS position::

    {"key":"LONGITUDE", "value":"", "type":"hidden"}


primary_key 
~~~~~~~~~~~~~

An item of particular importance, can be used by the application to link to particular infos::

    {"key":"tourism", "value":"", "type":"primary_key"}

Constraints
+++++++++++++++++

Constraints are conditions that are checked when the ok button of the form is pushed.

mandatory
~~~~~~~~~~

To make an item mandatory, just add::

    "mandatory": "yes"


range
~~~~~~~~~

To peform a range check on a numeric field you can add something like::

    "range":"[0,10)"

which would check that the inserted number is between 0 (inclusive) and 10 (exclusive).


Create a simple form to map fountains
-------------------------------------------

As an excercise we will now create a simple form to map fountains.

Sections
++++++++++++

Every form is composed of sections, each of which create a button in the 
:ref:`add notes <notes>` view.

We want to create a form for a fountain, so one section is enough.

The blueprint for such a form, i.e. the empty button shell starts with:

.. literalinclude:: 08_forms/tags.json
  :language: json                                                                
  :lines: 1-5

and ends with:

.. literalinclude:: 08_forms/tags.json
  :language: json                                                                
  :lines: 124-126

Form subsections
++++++++++++++++++++++

Each section can contain several sub-forms, that will create a tab each.

A sub-form starts with:

.. literalinclude:: 08_forms/tags.json
  :language: json                                                                
  :lines: 6-8

and ends with:

.. literalinclude:: 08_forms/tags.json
  :language: json                                                                
  :lines: 22-23

Note that the comma at the end is only needed if more than one sub-form
is added.

Form elements
+++++++++++++++++++

To add content to the sub-forms, any of the tags described :ref:`in the supported tags 
section <tags>` can be used.

For example lets add two textfields to prompt the user for a name and
street. Also the name should then be the label rendered in the map view.

.. literalinclude:: 08_forms/tags.json
  :language: json                                                                
  :emphasize-lines: 2,4,9
  :lines: 9-21

Finalize the form
++++++++++++++++++++++++

This is everything that needs to be done. Let's also add some technical data 
in a dedicated tab and also a tab for media, inside which it is possible to 
take pictures.

We leave the exercise to the reader.

Here below the tags file of a possible implementation is presented:

.. literalinclude:: 08_forms/tags.json
  :language: json                                                                



