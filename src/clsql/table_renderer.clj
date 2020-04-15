(ns clsql.table-renderer)

(defn- compute-row-widths [row] (map count row))

(defn- compute-column-widths [headers rows]
  (let [widths (apply conj
                      [(compute-row-widths headers)]
                      (map compute-row-widths rows))
        mapper (fn [index _] (apply max (map #(nth % index) widths)))]
    (first (map #(keep-indexed mapper %) widths))))

(defn- wrap-with
  ([wrapper] #(wrap-with wrapper %))
  ([wrapper string] (str wrapper string wrapper)))

(defn- pad-end [len pad value]
  (let [diff (- len (count value))]
    (if (>= diff 1)
      (apply str value (repeat diff pad))
      value)))

(defn- prepare-rows
  ([widths rows] (prepare-rows widths rows []))
  ([widths rows result]
   (when (seq rows)
     (let [row (first rows)
           rest (rest rows)
           padded-row (keep-indexed #(pad-end %2 " " (nth row %1)) widths)
           new-rows (conj result (map (wrap-with " ") padded-row))]
       (if (seq rest)
         (recur widths rest new-rows)
         new-rows)))))

(defn- create-decorator [begin middle divider end row]
  (let [middles (map #(apply str (repeat (count %) (str middle))) row)]
    (str begin (apply str (interpose divider middles)) end)))

(defn- decorate-row [row]
  (wrap-with \│ (apply str (interpose \│ row))))

(defn- make-header [headers has-rows?]
  (let [header (create-decorator \╒ \═ \╤ \╕ headers)
        body (decorate-row headers)
        bottom-divider (if has-rows? \╪ \╧)
        footer (create-decorator \╞ \═ bottom-divider \╡ headers)]
    (apply str (interpose "\n" [header body footer]))))

(defn render-table
  "Renders a table using provided headers and rows"
  [headers & rows]
  (let [column-widths (compute-column-widths headers rows)
        headers (first (prepare-rows column-widths [headers]))
        prepared-rows (prepare-rows column-widths rows)
        rows (apply str (interpose "\n" (map decorate-row prepared-rows)))
        has-rows? (seq rows)
        header (make-header headers has-rows?)
        bottom-divider (if has-rows? \╧ \═)
        footer (create-decorator \╘ \═ bottom-divider \╛ headers)]
    (apply str (interpose "\n" (remove empty? [header rows footer])))))
