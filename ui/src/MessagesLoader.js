class MessagesLoader {
  static get Empty() {
    return ((key) => "");
  }

  async load(keyArgs) {
    const lang = (window.navigator.languages && window.navigator.languages[0]) ||
          window.navigator.language ||
          window.navigator.userLanguage ||
          window.navigator.browserLanguage;

    try {
      const resp = await fetch(
        "/messages", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Csrf-Token": "nocheck",
            "Accept-language": lang
          },
          body: JSON.stringify({
            keyArgs: keyArgs
          })
        }
      );

      if (resp.status === 200) {
        this.messages = await resp.json();
        return (key) => {
          if (this.messages === undefined) return "";
          return this.messages[key];
        };
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }

    return MessagesLoader.Empty;
  }
}

export default MessagesLoader;
