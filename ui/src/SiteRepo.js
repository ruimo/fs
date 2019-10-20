class SiteRepo {
  static listToUpdate = (page, pageSize, orderBy, onSuccess, onLoginRequired, onUnknownError) =>
    SiteRepo.listSite("/api/listSiteToUpdate", page, pageSize, orderBy, onSuccess, onLoginRequired, onUnknownError)

  static list = (page, pageSize, orderBy, onSuccess, onLoginRequired, onUnknownError) =>
    SiteRepo.listSite("/api/listSite", page, pageSize, orderBy, onSuccess, onLoginRequired, onUnknownError)

  static listSite = async(urlBase, page, pageSize, orderBy, onSuccess, onLoginRequired, onUnknownError) => {
    try {
      const url = urlBase + "?page=" + page
            + "&pageSize=" + pageSize
            + "&orderBySpec=" + orderBy;
      const resp = await fetch(encodeURI(url));
      if (resp.status === 200) {
        const json = await resp.json();
        console.log("json: " + JSON.stringify(json));
        onSuccess(json['table']);
      } else if (resp.status === 401) {
        console.log("Login needed");
        onLoginRequired();
      } else {
        onUnknownError("error: " + resp.status);
      }
    } catch (e) {
      onUnknownError("error: " + JSON.stringify(e));
    }
  }
}

export default SiteRepo;

