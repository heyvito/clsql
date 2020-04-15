(ns clsql.errors)

(defn throw-if
  "Throws a CompilerException with a message if pred is true"
  [pred fmt & args]
  (when pred
    (let [^String message (apply format fmt args)
          exception (IllegalArgumentException. message)
          raw-trace (.getStackTrace exception)
          boring? #(not= (.getMethodName ^StackTraceElement %) "doInvoke")
          trace (into-array StackTraceElement (drop 2 (drop-while boring? raw-trace)))]
      (.setStackTrace exception trace)
      (throw exception))))
